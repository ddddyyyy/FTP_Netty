//
//  main.cpp
//  ftp_cpp
//
//  Created by 中山附一 on 2019/5/9.
//  Copyright © 2019 mdy. All rights reserved.
//

#include "network.h"
#include <iostream>
#include <string>
#include <stdint.h>
#include <Windows.h>
#include <time.h>
#include <sstream>
#include <thread>


CRITICAL_SECTION cs;//定义一个全局的锁 CRITICAL_SECTION的实例，可以理解为锁定一个资源
#pragma comment(lib, "ws2_32.lib")  //加载 ws2_32.dll
using namespace std;

#define BUFFER_SIZE 1024 * 2

DWORD WINAPI firstThread(__in LPVOID lpParameter)
{
	commandInput(lpParameter);
	return 0;
}

DWORD WINAPI senondThread(__in LPVOID lpParameter)
{
	dataRecv(lpParameter);
	return 0;
}


void connectToServer()
{
	WORD sockVersion = MAKEWORD(2, 2);
	WSADATA data;

	if (WSAStartup(sockVersion, &data) != 0)
	{
		return;
	}

	SOCKET sclient = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (sclient == INVALID_SOCKET)
	{
		printf("invalid socket!");
		return;
	}

	sockaddr_in serAddr;
	serAddr.sin_family = AF_INET;
	serAddr.sin_port = htons(666);
	serAddr.sin_addr.S_un.S_addr = inet_addr("127.0.0.1");
	if (connect(sclient, (sockaddr*)& serAddr, sizeof(serAddr)) == SOCKET_ERROR)
	{  //连接失败 
		printf("connect error !");
		closesocket(sclient);
		return;
	}


	std::thread task1(senondThread, &sclient);
	task1.detach();

	commandInput(&sclient);

	//HANDLE hThread[2];
	//InitializeCriticalSection(&cs); //初始化结构CRITICAL_SECTION

	//hThread[0] = CreateThread(NULL, 0, firstThread, &sclient, 0, NULL);//启动第一个线程
	//hThread[1] = CreateThread(NULL, 0, senondThread, &sclient, 0, NULL);//启动第二个线程

	//WaitForMultipleObjects(2, hThread, TRUE, INFINITE);//WaitForSingleObject(hThread, INFINITE);
	//cout << "this is end" << endl;
	//DeleteCriticalSection(&cs);

	//CloseHandle(hThread[0]);
	//CloseHandle(hThread[1]);
	//closesocket(sclient);
	//WSACleanup();

}


int main(int argc, const char* argv[])
{
	// insert code here...
	connectToServer();
	return 0;
}
