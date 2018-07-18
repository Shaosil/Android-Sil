/* File: main.c */

/*
 * Copyright (c) 1997 Ben Harrison, and others
 *
 * This software may be copied and distributed for educational, research,
 * and not for profit purposes provided that this copyright and statement
 * are included in all such copies.
 */

#include "angband.h"
#include "curses.h"

/*
 * Some machines have a "main()" function in their "main-xxx.c" file,
 * all the others use this file for their "main()" function.
 */


#if !defined(MACINTOSH) && !defined(WINDOWS) && !defined(RISCOS)

#include "main.h"

/*
 * Sil-y: game in progress
 */
bool game_in_progress = FALSE;

/*
 * List of the available modules in the order they are tried.
 */
static const struct module modules[] =
{
#ifdef USE_GTK
	{ "gtk", help_gtk, init_gtk },
#endif /* USE_GTK */

#ifdef USE_XAW
	{ "xaw", help_xaw, init_xaw },
#endif /* USE_XAW */

#ifdef USE_X11
	{ "x11", help_x11, init_x11 },
#endif /* USE_X11 */

#ifdef USE_XPJ
	{ "xpj", help_xpj, init_xpj },
#endif /* USE_XPJ */

#ifdef USE_GCU
	{ "gcu", help_gcu, init_gcu },
#endif /* USE_GCU */

#ifdef USE_CAP
	{ "cap", help_cap, init_cap },
#endif /* USE_CAP */

#ifdef USE_DOS
	{ "dos", help_dos, init_dos },
#endif /* USE_DOS */

#ifdef USE_IBM
	{ "ibm", help_ibm, init_ibm },
#endif /* USE_IBM */

#ifdef USE_EMX
	{ "emx", help_emx, init_emx },
#endif /* USE_EMX */

#ifdef USE_SLA
	{ "sla", help_sla, init_sla },
#endif /* USE_SLA */

#ifdef USE_LSL
	{ "lsl", help_lsl, init_lsl },
#endif /* USE_LSL */

#ifdef USE_AMI
	{ "ami", help_ami, init_ami },
#endif /* USE_AMI */

#ifdef USE_VME
	{ "vme", help_vme, init_vme },
#endif /* USE_VME */

#ifdef USE_VCS
	{ "vcs", help_vcs, init_vcs },
#endif /* USE_VCS */
};

/*
 * Set the stack size (for the Amiga)
 */
#ifdef AMIGA
# include <dos.h>
__near long __stack = 32768L;
#endif /* AMIGA */


/*
 * Set the stack size and overlay buffer (see main-286.c")
 */
#ifdef USE_286
# include <dos.h>
extern unsigned _stklen = 32768U;
extern unsigned _ovrbuffer = 0x1500;
#endif /* USE_286 */


#ifdef PRIVATE_USER_PATH

/*
 * Create an ".sil/" directory in the users home directory.
 *
 * ToDo: Add error handling.
 * ToDo: Only create the directories when actually writing files.
 */
static void create_user_dir(void)
{
	char dirpath[1024];
	char subdirpath[1024];


	/* Get an absolute path from the filename */
	path_parse(dirpath, sizeof(dirpath), PRIVATE_USER_PATH);

	/* Create the ~/.sil/ directory */
	mkdir(dirpath, 0700);

	/* Build the path to the variant-specific sub-directory */
	path_build(subdirpath, sizeof(subdirpath), dirpath, VERSION_NAME);

	/* Create the directory */
	mkdir(subdirpath, 0700);

#ifdef USE_PRIVATE_SAVE_PATH
	/* Build the path to the scores sub-directory */
	path_build(dirpath, sizeof(dirpath), subdirpath, "scores");

	/* Create the directory */
	mkdir(dirpath, 0700);

	/* Build the path to the savefile sub-directory */
	path_build(dirpath, sizeof(dirpath), subdirpath, "save");

	/* Create the directory */
	mkdir(dirpath, 0700);
#endif /* USE_PRIVATE_SAVE_PATH */
}

#endif /* PRIVATE_USER_PATH */

/*
 * Handle a "-d<what>=<path>" option
 *
 * The "<what>" can be any string starting with the same letter as the
 * name of a subdirectory of the "lib" folder (i.e. "i" or "info").
 *
 * The "<path>" can be any legal path for the given system, and should
 * not end in any special path separator (i.e. "/tmp" or "~/.ang-info").
 */
static void change_path(cptr info)
{
	cptr s;

	/* Find equal sign */
	s = strchr(info, '=');

	/* Verify equal sign */
	if (!s) quit_fmt("Try '-d<what>=<path>' not '-d%s'", info);

	/* Analyze */
	switch (tolower((unsigned char)info[0]))
	{
#ifndef FIXED_PATHS
		case 'a':
		{
			string_free(ANGBAND_DIR_APEX);
			ANGBAND_DIR_APEX = string_make(s+1);
			break;
		}

		case 'f':
		{
			//string_free(ANGBAND_DIR_FILE);
			//ANGBAND_DIR_FILE = string_make(s+1);
			break;
		}

		case 'h':
		{
			//string_free(ANGBAND_DIR_HELP);
			//ANGBAND_DIR_HELP = string_make(s+1);
			break;
		}

		case 'i':
		{
			//string_free(ANGBAND_DIR_INFO);
			//ANGBAND_DIR_INFO = string_make(s+1);
			break;
		}

		case 'x':
		{
			string_free(ANGBAND_DIR_XTRA);
			ANGBAND_DIR_XTRA = string_make(s+1);
			break;
		}

#ifdef VERIFY_SAVEFILE

		case 'b':
		case 'd':
		case 'e':
		case 's':
		{
			quit_fmt("Restricted option '-d%s'", info);
		}

#else /* VERIFY_SAVEFILE */

		case 'b':
		{
			//string_free(ANGBAND_DIR_BONE);
			//ANGBAND_DIR_BONE = string_make(s+1);
			break;
		}

		case 'd':
		{
			string_free(ANGBAND_DIR_DATA);
			ANGBAND_DIR_DATA = string_make(s+1);
			break;
		}

		case 'e':
		{
			string_free(ANGBAND_DIR_EDIT);
			ANGBAND_DIR_EDIT = string_make(s+1);
			break;
		}

		case 's':
		{
			string_free(ANGBAND_DIR_SAVE);
			ANGBAND_DIR_SAVE = string_make(s+1);
			break;
		}

#endif /* VERIFY_SAVEFILE */

#endif /* FIXED_PATHS */

		case 'u':
		{
			string_free(ANGBAND_DIR_USER);
			ANGBAND_DIR_USER = string_make(s+1);
			break;
		}

		default:
		{
			quit_fmt("Bad semantics in '-d%s'", info);
		}
	}
}


/*
 * Simple "main" function for multiple platforms.
 *
 * Note the special "--" option which terminates the processing of
 * standard options.  All non-standard options (if any) are passed
 * directly to the "init_xxx()" function.
 */
int main(int argc, char *argv[])
{
	int i;

	bool new_game = FALSE;

	int show_score = 0;

	cptr mstr = NULL;

	bool args = TRUE;
	bool quit_selected = FALSE;
	game_in_progress = FALSE;

	/* Save the "program name" XXX XXX XXX */
	argv0 = argv[0];

	/* Process the command line arguments */
	for (i = 1; args && (i < argc); i++)
	{
		cptr arg = argv[i];

		/* Require proper options */
		if (*arg++ != '-') goto usage;

		/* Analyze option */
		switch (*arg++)
		{
			case 'N':
			case 'n':
			{
				new_game = TRUE;
				
				// Sil-y:
				game_in_progress = TRUE;
				break;
			}

			case 'F':
			case 'f':
			{
				arg_fiddle = TRUE;
				break;
			}

			case 'W':
			case 'w':
			{
				arg_wizard = TRUE;
				break;
			}

			case 'V':
			case 'v':
			{
				arg_sound = TRUE;
				break;
			}

			case 'G':
			case 'g':
			{
				/* Default graphics tile */
				arg_graphics = GRAPHICS_ADAM_BOLT;
				break;
			}

			case 'R':
			case 'r':
			{
				arg_force_roguelike = TRUE;
				break;
			}

			case 'O':
			case 'o':
			{
				arg_force_original = TRUE;
				break;
			}

			case 'S':
			case 's':
			{
				show_score = atoi(arg);
				if (show_score <= 0) show_score = 10;
				continue;
			}

			case 'u':
			case 'U':
			{
				if (!*arg) goto usage;

				/* Get the savefile name */
				my_strcpy(op_ptr->full_name, arg, sizeof(op_ptr->full_name));

				// Sil-y:
				game_in_progress = TRUE;
				continue;
			}

			case 'm':
			case 'M':
			{
				if (!*arg) goto usage;
				mstr = arg;
				continue;
			}

			case 'd':
			case 'D':
			{
				change_path(arg);
				continue;
			}

			case '-':
			{
				argv[i] = argv[0];
				argc = argc - i;
				argv = argv + i;
				args = FALSE;
				break;
			}

			default:
			usage:
			{
				/* Dump usage information */
				puts("Usage: sil [options] [-- subopts]");
				puts("  -n       Start a new character");
				puts("  -f       Request fiddle (verbose) mode");
				puts("  -w       Request wizard mode");
				puts("  -v       Request sound mode");
				puts("  -g       Request graphics mode");
				puts("  -o       Request original keyset (default)");
				puts("  -r       Request rogue-like keyset");
				puts("  -s<num>  Show <num> high scores (default: 10)");
				puts("  -u<who>  Use your <who> savefile");
				puts("  -d<def>  Define a 'lib' dir sub-path");
				puts("  -m<sys>  use Module <sys>, where <sys> can be:");

				/* Print the name and help for each available module */
				for (i = 0; i < (int)N_ELEMENTS(modules); i++)
				{
					printf("     %s   %s\n",
					       modules[i].name, modules[i].help);
				}
				
				/* Actually abort the process */
				quit(NULL);
			}
		}
		if (*arg) goto usage;
	}

	/* Hack -- Forget standard args */
	if (args)
	{
		argc = 1;
		argv[1] = NULL;
	}

	/* Catch nasty signals */
	signals_init();

	/* Initialize */
	init_angband();

	/* Hack -- If requested, display scores and quit */
	if (show_score > 0) display_scores(0, show_score);

	/* Wait for response */
	//pause_line(Term->hgt - 1);

	/* Play the game */
	//play_game(new_game);

	// Sil-y: There is now a text menu that can play repeated games
	while (1)
	{
		/* Let the player choose a savefile or start a new game */
		if (!game_in_progress)
		{
			int choice = 0;
			int highlight = 1;
			char buf[1080];
			
			if (p_ptr->is_dead) highlight = 3;
			
			/* Process Events until "new" or "open" is selected */
			while (!game_in_progress && !quit_selected)
			{
				choice = initial_menu(&highlight);
				
				switch (choice)
				{
					case 1:
						path_build(savefile, sizeof(buf), ANGBAND_DIR_APEX, "tutorial");
						game_in_progress = TRUE;
						new_game = FALSE;
						break;
					case 2:
						game_in_progress = TRUE;
						new_game = FALSE;
						break;
					case 3:
						//cleanup_angband();
						//quit(NULL);
						quit_selected = TRUE;
						break;
				}
			}
		}

		if (quit_selected) break;
		
		/* Handle pending events (most notably update) and flush input */
		Term_flush();
		
		/*
		 * Play a game -- "new_game" is set by "new", "open" or the open document
		 * even handler as appropriate
		 */
		play_game(new_game);
		
		// rerun the first initialization routine
		extern void init_android_stuff(void);
		init_android_stuff();
		
		// do some more between-games initialization
		re_init_some_things();
		
		// game no longer in progress
		game_in_progress = FALSE;
	}
	
	/* Free resources */
	cleanup_angband();

	/* Quit */
	//quit(NULL);

	/* Exit */
	return (0);
}

#endif /* !defined(MACINTOSH) && !defined(WINDOWS) && !defined(RISCOS) */
