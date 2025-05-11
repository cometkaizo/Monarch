#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "common.h"
#include "vm.h"
#include "chunk.h"
#include "debug.h"
#include "input.h"
#include "strutil.h"

int printWorkingDir(void);
char* promptPath(void);
char* pathDir(char* path);
char* pathName(char* path);
char* prompt(void);

int main(int argc, const char* argv[]) {
	printf("Monarch VM\n");

	if (!printWorkingDir()) return 1;

	char* path = promptPath();
	if (!path) return 1;
	char* dir = pathDir(path);
	if (!dir) return 1;
	char* name = pathName(path);

	initVM(dir);

	readAndAddChunk(name);
	Chunk* c = getChunkByName(name);
	disassembleChunk(c, name);

	char* input = prompt();
	if (shouldRun(input)) {
		setVMDebugTrace(shouldDebug(input));
		setCurrentChunkByName(name);
		InterpretResult result = interpret();
		switch (result) {
		case INTERPRET_OK: 
			printf("Interpretation ended normally\n");
			break;
		case INTERPRET_ERROR: 
			printf("Interpretation encountered an error\n");
			break;
		}
	}

	freeVM();
	free(path);
	free(dir);
	return 0;
}

int printWorkingDir(void) {
	char workingDir[1024];
	if (getcwd(workingDir, sizeof(workingDir)) != NULL) {
		printf("Working directory: %s\n", workingDir);
		return 1;
	} else {
		perror("Error getting the working directory");
		return 0;
	}
}

char* promptPath(void) {
	char* input = prompt();
	if (strcmp(input, "test") == 0) {
		return strDyn("../../sample/mainentry.mnrc");
	}
	return input;
}
char* pathDir(char* path) {
	char* lastSeparator = strrchr(path, '/');
	size_t len = (size_t)(lastSeparator + 1 - path);
	char* dir = malloc(len + 1);
	if (!dir) return NULL;
	memcpy(dir, path, len);
	dir[len] = '\0';
	return dir;
}
char* pathName(char* path) {
	return strrchr(path, '/') + 1;
}

static char* prompt(void) {
	return readString(stdin, 10);
}

static int shouldRun(char* input) {
	return strcmp(input, "run") == 0 || strcmp(input, "debug") == 0;
}
static int shouldDebug(char* input) {
	return strcmp(input, "debug") == 0;
}