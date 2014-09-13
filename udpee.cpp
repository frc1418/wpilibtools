#include <iostream>
#include <fstream>
#include <list>

#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <ifaddrs.h>
#include <net/if.h>

void printInet4Address(sockaddr_in* s) {
	char name[50];
	inet_ntop(AF_INET, &s->sin_addr.s_addr, name, sizeof(name));
	std::cerr << name << std::endl;
}

std::list<in_addr> getBroadcastAddrs() {
	struct ifaddrs* addrs;
	std::list<in_addr> items;
	getifaddrs(&addrs);

	for (struct ifaddrs* ifa = addrs; ifa != NULL; ifa = ifa->ifa_next) {
		if (ifa->ifa_addr == 0) {
			continue;
		}
		if (ifa->ifa_addr->sa_family != AF_INET) {
			continue;
		}
		if (ifa->ifa_flags & IFF_BROADCAST) {
			sockaddr_in* addr = (sockaddr_in*) ifa->ifa_ifu.ifu_broadaddr;
			items.push_back(addr->sin_addr);
		}
	}

	freeifaddrs(addrs);

	return items;
}

class UDPSocket {
public:
	UDPSocket(std::string target, uint16_t port) {
		this->port = port;
		failed = false;
		addr.sin_family = AF_INET;
		addr.sin_addr.s_addr = 0;
		int success = inet_aton((char*) target.c_str(), &addr.sin_addr);
		if (!success) {
			std::cerr << "Address conversion failed";
			failed = true;
			return;
		}
		addr.sin_port = htons(port);

		sock = socket(AF_INET, SOCK_DGRAM, 0);
		if (sock == -1) {
			std::cerr << "Socket creation failed: Errno " << errno << std::endl;
			failed = true;
			return;
		}
		if (target == "255.255.255.255") {
			broadcast = true;
			int optval = 1;
			int err = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &optval,
                                 sizeof(optval));
			if (err == -1) {
				std::cerr << "Setting socket to broadcast: Errno " << errno
                          << std::endl;
				failed = true;
				close(sock);
				return;
			}
		} else {
			broadcast = false;
		}
	}
	~UDPSocket() {
		if (!failed) {
			close(sock);
        }
	}
    static void send(int sock, const std::string& data, sockaddr_in* sa) {
		ssize_t sent = sendto(sock, data.c_str(), data.length(), 0,
                              (sockaddr*) sa, sizeof(*sa));
		if (sent == -1) {
			std::cerr << "Packet send failed: Errno " << errno << std::endl;
		}
	}
	void write(std::string data) {
		if (!failed) {
            if (broadcast) {
				std::list<in_addr> addrs = getBroadcastAddrs();
				std::list<in_addr>::iterator iend = addrs.end();
				for (std::list<in_addr>::iterator i = addrs.begin(); i != iend; ++i) {
					sockaddr_in sa;
					sa.sin_port = htons(port);
					sa.sin_family = AF_INET;
					sa.sin_addr = *i;
					send(sock, data, &sa);
				}
			} else {
				send(sock, data, &addr);
			}
		}
	}
private:
	bool failed;
	int sock;
	bool broadcast;
	uint16_t port;
	sockaddr_in addr;
};

std::string modes[][2] = { { "loopback", "127.0.0.1" }, { "broadcast",
        "255.255.255.255"
    }, { "multicast", "224.0.0.1" }
};

void help() {
	std::string opts(modes[0][0]);
	for (uint i = 1; i < sizeof(modes) / sizeof(modes[0]); i++) {
		opts.push_back('|');
		opts.append(modes[i][0]);
	}
	std::cerr << "Usage: udpee [" << opts << "] [LOGFILE]" << std::endl;
	std::cerr << "Reads standard input, sends it via UDP to port" << std::endl;
	std::cerr << "6666, and logs it to a file and to standard output"
              << std::endl;
}

int main(int argc, const char** argv) {
	if (argc != 2 && argc != 3) {
		help();
		return -1;
	}

	std::string ip;
	std::string mode(argv[1]);
	for (uint i = 0; i < sizeof(modes) / sizeof(modes[0]); i++) {
		if (mode == modes[i][0]) {
			ip = modes[i][1];
		}
	}
	if (ip.empty()) {
		help();
		return -1;
	}

	bool withfile = argc == 3;

	std::string packet;
	{
		UDPSocket sock(ip, 6666);
		if (withfile) {
			std::ofstream output(argv[2]);
			while (std::getline(std::cin, packet)) {
				std::cout << packet << std::endl;
                packet.append("\n");
				output << packet;
				sock.write(packet);
			}
		} else {
			while (std::getline(std::cin, packet)) {
				std::cout << packet << std::endl;
				sock.write(packet.append("\n"));
			}
		}
	}

	return 0;

}
