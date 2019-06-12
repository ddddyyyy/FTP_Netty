//
//  main.cpp
//  ftp_cpp
//
//  Created by 中山附一 on 2019/5/9.
//  Copyright © 2019 mdy. All rights reserved.
//

#include <iostream>
#include <string>
#include <stdint.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include "command.pb.h"

using namespace std;

#define BUFFER_SIZE 1024 * 2

void connectToServer();
void connectToServer()

{
    ///定义sockfd

    int sock_cli = socket(AF_INET, SOCK_STREAM, 0);

    ///定义sockaddr_in

    struct sockaddr_in servaddr;

    memset(&servaddr, 0, sizeof(servaddr));

    servaddr.sin_family = AF_INET;

    servaddr.sin_port = htons(666); ///服务器端口

    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1"); ///服务器ip

    ///连接服务器，成功返回0，错误返回-1

    if (connect(sock_cli, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)

    {

        perror("connect");

        exit(1);
    }

    //

    printf("连接服务器成功\n");

    char sendbuf[BUFFER_SIZE];

    char recvbuf[BUFFER_SIZE];

    printf("等待输入中\n");

    ftp::Request request;
    request.set_command(ftp::Request_Type_LS);
    string s;
    request.SerializeToString(&s);
    send(sock_cli, s.c_str(), strlen(s.c_str()), 0); ///发送
    recv(sock_cli, recvbuf, sizeof(recvbuf), 0); ///接收
    printf("接收数据:%s\n", sendbuf);

    while (fgets(sendbuf, sizeof(sendbuf), stdin) != NULL)

    {

        printf("等待输入是否阻塞线程\n");

        send(sock_cli, sendbuf, strlen(sendbuf), 0); ///发送

        printf("发送数据:%s\n", sendbuf);

        if (strcmp(sendbuf, "exit\n") == 0)

        {

            printf("发送退出信息\n");

            break;
        }

        recv(sock_cli, recvbuf, sizeof(recvbuf), 0); ///接收

        printf("接收数据:%s\n", sendbuf);

        //        fputs(recvbuf, stdout);

        memset(sendbuf, 0, sizeof(sendbuf));

        memset(recvbuf, 0, sizeof(recvbuf));
    }

    close(sock_cli);
}

int main(int argc, const char *argv[])
{
    // insert code here...
    //GOOGLE_PROTOBUF_VERIFY_VERSION;
    //connectToServer();
    return 0;
}
