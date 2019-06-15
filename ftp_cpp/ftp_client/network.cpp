#include "network.h"
#include "command.pb.h"
#include <iostream>
#include <string>
#include <WinSock2.h>
#include <typeinfo>
#include<sstream>
#include <fstream>
#include "MD5Util.h"

using namespace std;

#define MAX_SIZE 1024 * 2

string method;
string md5;
string filePath;
std::ofstream fout;
std::ifstream fin;
bool check = false;
long fileSize;

void cleanUp(SOCKET sclient) {
	check = false;
	method = "";
	md5 = "";
	filePath = "";
	closesocket(sclient);
	fout.close();
	fin.close();
	fileSize = 0;
}

char* long2Byte(long n)
{
	char c[8];
	for (int i = 0; i < 8; ++i)
	{
		int offset = 64 - (i + 1) * 8;
		c[i] = (char)((n >> offset) & 0xff);
	}
	return c;
}

char* int2Byte(int n)
{
	char c[4];
	for (int i = 0; i < 4; ++i)
	{
		int offset = 32 - (i + 1) * 8;
		c[i] = (char)((n >> offset) & 0xff);
	}
	return c;
}

int byte2Int(char* c)
{
	int n = 0;
	for (int i = 0; i < 4; ++i)
	{
		n <<= 8;
		n |= (c[i] & 0xff);
	}
	return n;
}


void analysisResponse(char* data, int len)
{
	ftp::Response response;
	response.ParseFromArray(data, len);
	switch (response.status())
	{
	case ftp::Response_Status_NOT_LOGIN:
		cout << "需要登陆" << endl;
		break;
	case ftp::Response_Status_FILE_LIST:
		cout << response.msg() << endl;
		break;
	case ftp::Response_Status_PASV:
	{
		//开启数据连接线程
		char root[] = "C:\\Users\\21156\\source\\repos\\ddddyyyy\\FTP_Netty\\ftp_file_receive\\";
		if (!fileServe(root, (char*)response.msg().c_str())) {
			cout << "文件传输启动失败" << endl;
		}
		else {
			filePath = root + filePath;
		}
	}
		break;
	default:
		cout << ftp::Response::Status_Name(response.status()) << endl;
	}
}

void checkFileComplete(SOCKET client)
{
	if (MD5_file((char*)filePath.c_str(), 32) == md5) {
		ftp::Data data;
		int len;
		char* d = obj2Byte(&data, len, 0x00);
		if (send(client, d, len, 0) > 0)
		{
			cout << "文件接受成功" << endl;
			cleanUp(client);
		}
	}
}

void analysisData(char* recData, int len,void* obj) {
	SOCKET* sclient = (SOCKET*)obj;

	ftp::Data data;
	data.ParseFromArray(recData, len);

	cout << ftp::Data::Status_Name(data.status()) << endl ; 

	switch (data.status())
	{
	case ftp::Data_Status_SUCCESS:
		if (method == "get") {
			//文件下载
			fout.open(filePath, ios::binary | ios::out | ios::in | ios::trunc);
			if (!fout.bad()) {
				int size = byte2Int((char*)data.data().c_str() + 4);
				fout.seekp(size, ios::beg);
				fout.flush();
			}
			else {
				cout << "file open error" << endl;
			}
			
			ftp::Data d;
			d.set_status(ftp::Data_Status_GET);
			int length;
			char* temp = obj2Byte(&d, length, 0x02);
			if (send(*sclient, temp, length, 0) < 0) {
				cout << "inform error" << endl;
			}
		}
		else
		{
			//文件上传
			ftp::Data d;
			d.set_status(ftp::Data_Status_READY);

			fin.open(filePath, ios::binary | ios::in);
			fin.seekg(0, ios::end);
			fileSize = fin.tellg();
			fin.seekg(0, ios::beg);

			d.set_data(long2Byte(fileSize),8);

			for (int i = 0; i < 8; ++i) {
				printf("%d  ", d.data()[i]);
			}

			int length;
			char* temp = obj2Byte(&d, length, 0x02);
			if (send(*sclient, temp, length, 0) < 0) {
				cout << "inform error" << endl;
			}
		}
		break;
	case ftp::Data_Status_GET:
	{
		cout << "开始上传" << endl;

		ftp::Data d;
		d.set_status(ftp::Data_Status_STORE);

		long pos = 0;
		char buffer[MAX_SIZE];

		while (!fin.eof()) {

			fin.read(buffer, MAX_SIZE);

			d.set_pos(pos++);

			if (fileSize < MAX_SIZE * pos) {
				//文件尾
				d.set_data(buffer, fileSize - (pos - 1) * MAX_SIZE);
			}
			else
				d.set_data(buffer, MAX_SIZE);
			int length;
			char* temp = obj2Byte(&d, length, 0x02);
			if (send(*sclient, temp, length, 0) > 0) {
				cout << pos << endl;
			}
		}

		d.set_pos(pos);
		d.set_data("");
		int length;
		char* temp = obj2Byte(&d, length, 0x02);
		send(*sclient, temp, length, 0);

		d.set_status(ftp::Data_Status_MD5);
		d.set_data(MD5_file((char*)filePath.c_str(), 32));
		temp = obj2Byte(&d, length, 0x02);
		send(*sclient, temp, length, 0);

		cout << "结束上传" << endl;
	}
		break;
	case ftp::Data_Status_MD5:
		md5 = data.data();
		checkFileComplete(*sclient);
		break;
	case ftp::Data_Status_STORE:
		if (data.data() == "") {
			//为空
		}
		else {
			fout.seekp(data.pos() * MAX_SIZE, ios::beg);
			fout.write(data.data().c_str(), data.data().size());
			fout.flush();
		}
		if (md5 != "") {
			checkFileComplete(*sclient);
		}
		break;
	case ftp::Data_Status_FIN:
		cleanUp(*sclient);
		break;
	default:
		break;
	}
}

// 将对象转为二进制
char* obj2Byte(void* obj, int& len, char type)
{
	string str;

	switch (type)
	{
	case 0x01:
		(*((ftp::Response*)(obj))).SerializeToString(&str);
		break;
	case 0x02:
		((ftp::Data*)(obj))->SerializeToString(&str);
		break;
	case 0x00:
		((ftp::Request*)(obj))->SerializeToString(&str);
		break;
	default:
		break;
	}

	len = 1 + 4 + str.length();

	const char* length = int2Byte(str.length());//数据长度
	const char* body = str.c_str();   //要发送的数据

	char* sendData = new char[len];

	sendData[4] = type;


	sendData[0] = length[0];
	sendData[1] = length[1];
	sendData[2] = length[2];
	sendData[3] = length[3];

	for (int i = 0; i < len - 5; ++i)
	{
		sendData[5 + i] = body[i];
	}

	return sendData;
}


void trim(string& s)
{
	if (s.empty())
	{
		return;
	}
	s.erase(0, s.find_first_not_of(" "));
	s.erase(s.find_last_not_of(" ") + 1);
}


//发送数据
void commandInput(void* obj) {
	SOCKET* sclient = (SOCKET*)obj;

	//char command[100];
	string command;
	int len;

	while (true)
	{
		ftp::Request request;
		bool sendOrNot = true;
		getline(cin, command);
		trim(command);
		//transform(command.begin(), command.end(), command.begin(),::tolower);//转小写
		stringstream ss(command);
		//暂存从command中读取的字符串 
		string result;
		//用于存放分割后的字符串 
		vector<string> res;
		while (ss >> result)
			res.push_back(result);
		if (res.size() < 1) {
			continue;
		}
		else if (res[0] == "ls") {
			request.set_command(ftp::Request_Type_LS);
		}
		else if (res[0] == "quit") {
			WSACleanup();
			exit(0);
		}
		else if (res[0] == "user" && res.size() == 2) {
			request.set_command(ftp::Request_Type_USER);
			request.set_args(res[1].c_str());
		}
		else if (res[0] == "pass" && res.size() == 2) {
			request.set_command(ftp::Request_Type_PASS);
			request.set_args(res[1].c_str());
		}
		else if (res[0] == "put" && res.size() == 2) {
			request.set_command(ftp::Request_Type_PUT);
			request.set_args(res[1].c_str());
			method = "put";
			filePath = res[1];
		}
		else if (res[0] == "get" && res.size() == 2) {
			request.set_command(ftp::Request_Type_GET);
			request.set_args(res[1].c_str());
			method = "get";
			filePath = res[1];
		}
		else
		{
			sendOrNot = false;
			cout << "无效输入" << endl;
		}
		if (sendOrNot) {
			char* d = obj2Byte(&request, len, 0x00);
			if (send(*sclient, d , len, 0) < 0) {
				perror("send error");
				cleanUp(*sclient);
				return;
			}
		}
	}
}


//接收数据
void dataRecv(void* obj) {
	SOCKET* sclient = (SOCKET*)obj;

	char* recData;
	
	while (true)
	{
		recData = new char[5];
		if (recv(*sclient, recData, 5, 0) > 0)
		{
			int len = byte2Int(recData);//长度
			if (len == 0) 
			{
				continue;
			}
			char type = recData[4];//类型
			//接收对象
			recData = new char[len];
			if (recv(*sclient, recData, len, 0) > 0)
			{
				switch (type)
				{
				case 0x01: //响应数据包
					analysisResponse(recData, len);
					break;
				case 0x02://文件数据包
					//for (int i = 0; i < len; ++i) {
					//	printf("%d  ", recData[i]);
					//}
					//cout << endl << len << endl;
					analysisData(recData, len, sclient);
					break;
				default:
					break;
				}
			}
			else
			{
				cout << "recive body error,data len is " << len << endl;
				perror("send error");
				cleanUp(*sclient);
				break;
			}
		}
		else
		{
			cout << "recive head error" << endl;
			perror("send error");
			cleanUp(*sclient);
			break;
		}
	}
}



bool fileServe(char* root,char* port)
{
	WORD sockVersion = MAKEWORD(2, 2);
	WSADATA data;

	if (WSAStartup(sockVersion, &data) != 0)
	{
		return false;
	}

	SOCKET sclient = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (sclient == INVALID_SOCKET)
	{
		printf("invalid socket!");
		return false;
	}

	sockaddr_in serAddr;
	serAddr.sin_family = AF_INET;
	serAddr.sin_port = htons(atoi(port));
	serAddr.sin_addr.S_un.S_addr = inet_addr("127.0.0.1");
	if (connect(sclient, (sockaddr*)& serAddr, sizeof(serAddr)) == SOCKET_ERROR)
	{  //连接失败 
		printf("connect error !");
		closesocket(sclient);
		return false;
	}
	//开始接收数据
	std::thread task(dataRecv, &sclient);
	task.detach();

	return true;
}