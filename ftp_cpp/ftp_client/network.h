#pragma once
#ifndef NETWORK_H
#define NETWORK_H


char* int2Byte(int n);
int byte2Int(char* c);

void analysisResponse(char*,int);
void analysisData(char*, int,void*);

void commandInput(void*);

void dataRecv(void*);

bool fileServe(char*,char*);

char* obj2Byte(void*, int&,char);

#endif // !NETWORK_H
