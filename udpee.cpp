#include <iostream>
#include <fstream>

#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

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
            failed = true;
            return;
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
            if (sent != (ssize_t)data.length()) {
                std::cerr << "Packet send failed";
                failed = true;
                close(sock);
            }
        }
    }
private:
    bool failed;
    int sock;
    sockaddr_in addr;
};


int main(int argc, const char** argv)
{
    if (argc != 2) {
        std::cerr << "Usage: [STREAM] | udpee [LOGFILE]" << std::endl;
        return -1;
    }

    std::string packet;
    {
        std::ofstream output(argv[1]);

#if 0
        UDPSocket sock("224.0.0.1", 6666);
#else
        UDPSocket sock("127.0.0.1", 6666);
#endif

        while (std::getline(std::cin, packet)) {
            std::cout << packet << std::endl;
            output << packet << std::endl;
            sock.write(packet.append("\n"));
        }
    }

    return 0;

}
