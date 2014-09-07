#include <iostream>
#include <fstream>

#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>

class UDPSocket
{
public:
    UDPSocket(std::string target, uint16_t port)
    {
        failed = false;
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = 0;
        int success = inet_aton((char*)target.c_str(), &addr.sin_addr);
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
            int optval = 1;
            int err = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &optval, sizeof(optval));
            if (err == -1) {
                std::cerr << "Setting socket to broadcast: Errno " << errno << std::endl;
                failed = true;
                close(sock);
                return;
            }
        }
    }
    ~UDPSocket()
    {
        if (!failed) {
            close(sock);
        }
    }
    void write(std::string data)
    {
        if (!failed) {
            ssize_t sent = sendto(sock, data.c_str(), data.length(), 0, (sockaddr*) &addr,
                                  sizeof(addr));
            if (sent == -1) {
                std::cerr << "Packet send failed: Errno " << errno << std::endl;
            }
        }
    }
private:
    bool failed;
    int sock;
    sockaddr_in addr;
};

std::string modes[][2] = {
    {"loopback", "127.0.0.1"},
    {"broadcast", "255.255.255.255"},
    {"multicast", "224.0.0.1"}
};

void help()
{
    std::string opts(modes[0][0]);
    for (uint i = 1; i < sizeof(modes) / sizeof(modes[0]); i++) {
        opts.push_back('|');
        opts.append(modes[i][0]);
    }
    std::cerr << "Usage: udpee [" << opts << "] [LOGFILE]" << std::endl;
    std::cerr << "Reads standard input, sends it via UDP to port" << std::endl;
    std::cerr << "6666, and logs it to a file and to standard output" << std::endl;
}

int main(int argc, const char** argv)
{
    if (argc != 3) {
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

    std::string packet;
    {
        std::ofstream output(argv[2]);
        UDPSocket sock(ip, 6666);
        while (std::getline(std::cin, packet)) {
            std::cout << packet << std::endl;
            output << packet << std::endl;
            sock.write(packet.append("\n"));
        }
    }

    return 0;

}
