#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <setjmp.h>

#define COLOR_BLACK 0
#define COLOR_BLUE 1
#define COLOR_GREEN 2
#define COLOR_CYAN 3
#define COLOR_RED 4
#define COLOR_MAGENTA 5
#define COLOR_YELLOW 6
#define COLOR_WHITE 7

#define COLOR_GRAY 8
#define COLOR_LIGHT_BLACK 8
#define COLOR_LIGHT_BLUE 9
#define COLOR_LIGHT_GREEN 10
#define COLOR_LIGHT_CYAN 11
#define COLOR_LIGHT_RED 12
#define COLOR_LIGHT_MAGENTA 13
#define COLOR_LIGHT_YELLOW 14
#define COLOR_LIGHT_WHITE 15

#define A_NORMAL 0
#define A_REVERSE 0x100
#define A_STANDOUT 0x200
#define A_BOLD 0x400
#define A_UNDERLINE 0x800
//#define A_BLINK 0x1000
//#define A_DIM 0x2000
//#define A_ALTCHARSET 0x4000

#ifndef TRUE
#define TRUE -1
#define FALSE 0
#endif

#define COLOR_PAIR(x) (x)

#define LINES 24
#define COLS 80

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Sil", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "Sil", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "Sil", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "Sil", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "Sil", __VA_ARGS__) 
#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG  , "Sil", __VA_ARGS__)

typedef struct WINDOW_s {
	int w;
} WINDOW;
extern WINDOW* stdscr;

#define ERR 1
#define getyx(w, y, x)     (y = getcury(w), x = getcurx(w))

int addch(const char, const signed char, const char);
int addwch(const char, const wchar_t, const char);
int delch();
int waddch(WINDOW*, const char, const signed char, const char);
int waddwch(WINDOW*, const char, const wchar_t, const char);
int addstr(const char, const signed char *, const char);
int addwstr(const char, const wchar_t *, const char);
int waddstr(WINDOW*, const char, const signed char *, const char);
int waddwstr(WINDOW *, const char, const wchar_t *, const char);
int addnstr(int, int, const signed char *, const char);
int addnwstr(int, const char, const wchar_t *, const char);
int waddnstr(WINDOW*, int, int, const signed char *, const char);
int waddnwstr(WINDOW*, const char, int, const wchar_t *, const char);
int move(int, int);
int mvaddch(int, int, const char, const char, const char);
int mvaddwch(int, int, const char, const wchar_t, const char);
int mvwaddch(WINDOW *,int, int, const char, const char, const char);
int mvwaddwch(WINDOW *,int, int, const char, const wchar_t, const char);
int mvaddstr(int, int, const char, const signed char *, const char);
int mvaddwstr(int, int, const char, const wchar_t *, const char);
int whline(WINDOW*, const char, const char, int);
int hline(const char, const char, int);
int wclrtobot(WINDOW*);
int clrtobot(void);
int clrtoeol(void);
int wclrtoeol(WINDOW*);
#ifndef NO_CLEAR
int clear(void);
#endif
int wclear(WINDOW*);
int initscr(void);
int curs_set(int);
WINDOW* newwin(int,int,int,int);
int getcurx(WINDOW *);
int getcury(WINDOW *);
int overwrite(const WINDOW *, WINDOW *);
int touchwin(WINDOW *);
int delwin(WINDOW *);
int refresh(void);
int mvinch(int, int);
int mvwinch(WINDOW*, int, int);
int crmode();
int nonl();
int noecho();
int nl();
int echo();
int cbreak();
int nocbreak();
int notimeout(WINDOW *, int);
int endwin();
int has_colors();
int scrollok(WINDOW *, int); 
int scroll(WINDOW *); 
int intrflush(WINDOW *, int);
int beep(void);
int keypad(WINDOW *, int);

int sil_getch(int v);
int flushinp(void);
int noise(void);

void sil_quit(const char*);
void sil_warn(const char*);

#ifdef USE_MY_STR
size_t my_strcpy(char *, const char *, size_t);
size_t my_strcat(char *, const char *, size_t);
#endif

/* game must implement these */
void sil_process_argv(int, const char*);
void sil_main(void);
int queryInt(const char* argv0);

/* game may implement these */
extern void (*sil_
	)(void);
