# Android-Sil
An unofficial port of the roguelike Sil to Android

<b>This repository is all of the latest source code for <a href="https://play.google.com/store/apps/details?id=com.gmail.ShaosilDev.Sil.SilActivity">Sil on the Play Store</a></b>

Since this was my first Android app, and I was much more used to Visual Studio, I used that as the IDE rather than Android Studio.

Most of the code for the app came from the Angdroid port for Angband <a href="https://github.com/sergeybe/angdroid">(GitHub link)</a>, with a little help from the DC:SS Android port <a href="https://github.com/michaelbarlow7/dungeon-crawl-android">(GitHub link)</a> as well. The latest source code for Sil was downloaded from the <a href="http://www.amirrorclear.net/flowers/game/sil/index.html">home site</a>, with minor changes. I plan to keep this app up to date with future Sil releases, if any.

<b>Things that have changed from the code I referenced</b>
<ul>
<li>Upgraded Sil to version 1.3</li>
<li><b>APP BUGFIX:</b> Save games are no longer being corrupted! This was the highest thing to fix on my list. We can't be having saves be corrupted for a game like this. The other app was calling a save function any time the app lost focus, and didn't always detect whether the game was in progress. Sometimes, it would save during the main menu and overwrite the game data. I just improved the in progress logic.</li>
<li><b>APP BUGFIX:</b> The quit button now functions from the main menu.</li>
<li><b>APP BUGFIX:</b> With the introduction of an overlaying keyboard, the 9 "squares" on the screen that are used for finger tap movement didn't work when the transparent keyboard is overtop of them.</li>
<li><b>APP BUGFIX:</b> Slain monsters list didn't appear</li>
<li><b>SIL BUGFIX:</b> Playing a game after the tutorial no longer displays a blank message at the top.</li>
<li><b>SIL BUGFIX:</b> Trying to save during the tutorial no longer displays a warning message with -more- cutting out the middle.
<li>Added a tab key to the symbols keyboard.</li>
<li>Added an options key to the symbols keyboard.</li>
<li>The orientation preference now has four options: Sensor (System), Sensor (Forced), Portrait, and Landscape. Sensor (System) will rotate based on the current user rotate system preference, and System (Forced) will rotate based on the sensor, regardless of user preference. The latter was the only sensor setting previously, and it bugged me.</li>
<li>The keyboard no longer displays annoying little popup key animations between certain keypresses. I had to hack my way around this one because that animation code is part of Android's functionality for that class.</li>
<li>Raised the minimum and target APIs a bit (19 and 26 - KitKat and Oreo). As a result, I had to make sure the newer Android permission system worked and asked the user about permissions when needed.</li>
<li>Added original font. I left the new font alone, because after I painstakingly traced each bitmap character in the original 8x13 font to convert it to vector (android only likes vector fonts), I realized how truly awful it looks on phone screens. However, I did add a font option to preferences if you must have the classic look.</li>
<li>Fixed background colors. This means walls draw as solid blocks (assuming you have the option enabled), highlights work, etc.
<li>Added transparency option for keyboard, along with the option to overlay it on top of the game view. Great for playing in landscape mode, similar to what you can do with DC:SS.</li>
<li>Introduced and then fixed a rather large bug related to the graphics being drawn. Still not sure what causes it, and why it doesn't happen with Angband Variants and DC:SS, since they use nearly identical code. If you pressed keys too fast, the game bitmap could be updated during a draw, causing minor glitches like overlapping or half-blanked-out text. I fixed it by rewriting the draw method to use a queue of characters that were set instead of external draw calls.</li>
<li>Other minor code improvements that shouldn't affect much.</li>
</ul>

<b>Things I may or may not get to someday</b>
<ul>
<li>Add way to view manual from app, or at least link to it.</li>
<li>Add optional visible guidelines to the part of the screen which is tappable, if using tap movement.</li>
<li>A single row sliding keyboard would be nice, though I think the transparent option helps a lot, so it may be more effort than it's worth.</li>
<li>Want a way to copy over ALL settings from last character on a new one, currently the skills are zeroed out.</li>
<li>Filling the whole screen with the game view may be useful, but I'm not even sure if Sil would allow that type of aspect ratio.</li>
<li>A way to drag your finger around to view the game map would be nice, but may require too many changes to Sil's code.</li>
</ul>
