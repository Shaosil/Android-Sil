package com.gmail.ShaosilDev.Sil;

import android.util.Log;

public class TermWindow {

	public static int TERM_BLACK = 0xFF000000;
	public static int TERM_WHITE = 0xFFFFFFFF;

	public class TermPoint {
		public char Char = ' ';
		public byte Flag = 0;
		public int Color = 0;
		public boolean isDirty = false;
		public boolean isUgly = false;
	}
	public TermPoint[][] buffer = null; 

	public boolean allDirty = false;
	public boolean cursor_visible;
	public int col = 0;
	public int row = 0;

	public int cols = 0;
	public int rows = 0;
	public int begin_y = 0;
	public int begin_x = 0;

	public TermWindow(int rows, int cols, int begin_y, int begin_x) {
		if (cols == 0) this.cols = Preferences.cols;
		else this.cols = cols;
		if (rows == 0) this.rows = Preferences.rows;
		else this.rows = rows;
		this.begin_y = begin_y;
		this.begin_x = begin_x;
		buffer = new TermPoint[this.rows][this.cols];
		for(int r=0;r<this.rows;r++) {
			for(int c=0;c<this.cols;c++) {
				buffer[r][c] = newPoint(null);
			}
		}
	}

	private TermPoint newPoint(TermPoint p) {
		if (p == null) 
			p = new TermPoint();
		else {
			p.isDirty = p.isDirty || p.Char != ' ' || p.Color != 0 || p.Flag != 0;
			p.Char = ' ';
			p.Color = 0;
			p.Flag = 0;
		}
		return p;
	}

	public void clearPoint(int row, int col) {
		if (col>-1 && col<cols 
			&& row>-1 && row<rows) {
			TermPoint p = buffer[row][col];
			newPoint(p);
		}
		else {
			Log.d("Sil","TermWindow.clearPoint - point out of bounds: "+col+","+row);
		}
	}

	public void clear() {
		for(int r=0;r<rows;r++) {
			for(int c=0;c<cols;c++) {
				//Log.d("Sil","TermWindow.clear clearPoint "+r+","+c);
				TermPoint p = buffer[r][c];
				newPoint(p);
			}
		}
		//Log.d("Sil","TermWindow.clear end");
	}

	public void clrtoeol() {
		for(int c=col;c<cols;c++) {
			TermPoint p = buffer[row][c];
			newPoint(p);
		}
	}

	public void clrtobot() {
		for(int r=row;r<rows;r++) {
			for(int c=col;c<cols;c++) {
				TermPoint p = buffer[r][c];
				newPoint(p);
			}
		}
	}

	public void hline(byte a, char c, int n) {
		int x = Math.min(cols,n+col);
		for(int i=col;i<x;i++) {
			addch(a, c, (byte)0);
		}
	}

	public void move(int row, int col) {
		if (col>-1 && col<cols && row>-1 & row<rows) {
			this.col = col;
			this.row = row;
		}
	}

	public int inch() {
		return buffer[this.row][this.col].Char;
	}

	public int mvinch(int row, int col) {
		move(row,col);
		return inch();
	}

	public int attrget(int row, int col) {
		if (col>-1 && col<cols && row>-1 & row<rows) {
			return buffer[row][col].Color;
		}
		return -1;
	}

	public int getcury() {
		return row;
	}

	public int getcurx() {
		return col;
	}

	public void overwrite(TermWindow wsrc) {
		int sx0 = wsrc.begin_x;
		int sx1 = wsrc.begin_x+wsrc.cols-1;
		int sy0 = wsrc.begin_y;
		int sy1 = wsrc.begin_y+wsrc.rows-1;
		int dx0 = begin_x;
		int dx1 = begin_x+cols-1;
		int dy0 = begin_y;
		int dy1 = begin_y+rows-1;

		// do wins intersect?
		if (!(sx0 > dx1 || sx1 < dx0 || sy0 > dy1 || sy1 < dy0)) {

			// calc intersect area
			int ix0 = Math.max(sx0,dx0);
			int iy0 = Math.max(sy0,dy0);
			int ix1 = Math.min(sx1,dx1);
			int iy1 = Math.min(sy1,dy1);

			// blit the ascii
			for(int r=iy0;r<=iy1;r++) {
				for(int c=ix0;c<=ix1;c++) {
					TermPoint p1 = wsrc.buffer[r-wsrc.begin_y][c-wsrc.begin_x];
					TermPoint p2 = buffer[r-begin_y][c-begin_x];
					p2.isDirty = p2.isDirty || p2.Char != p1.Char || p2.Color != p1.Color || p2.Flag != p1.Flag;
					p2.Char = p1.Char;
					p2.Color = p1.Color;
					p2.Flag = p1.Flag;
				}
			}
		}
	}

	public void touch() {
		for(int r=0;r<rows;r++) {
			for(int c=0;c<cols;c++) {
				TermPoint p = buffer[r][c];
				p.isDirty = true;
			}
		}
	}

	public void addnstr(int clr, int n, byte[] cp, byte flag) {
		String str;
		try {
			str = new String(cp, "ISO-8859-1");
			//Log.d("TermWindow.addnstr","addnstr ("+row+","+col+") ["+str+"]");
		} catch(java.io.UnsupportedEncodingException e) {
			str = "";
		}
		
		for (int i = 0; i < n; i++) {
		    addch(clr, str.charAt(i), flag);
		}
	}

	public void addch(int clr, char c, byte flag) {
		if (col>-1 && col<cols && row>-1 && row<rows) {
			if (c > 19) {
				TermPoint p = buffer[row][col];
			
				p.isDirty = p.isDirty || p.Char != c || p.Color != clr;
				p.Char = c;
				p.Flag = flag;
				p.Color = clr;
				advance();
			}
			else if (c == 9) {  // recurse to expand that tab
				//Log.d("Sil","TermWindow.addch - tab expand");				
				int ss = col % 8;
				if (ss==0) ss=8;
				for(int i=0;i<ss;i++) addch((byte)0, ' ', (byte)0);
			}
			else {
				//Log.d("Sil","TermWindow.addch - invalid character: "+(int)c);
				advance();
			}
		}
		else {
			Log.d("Sil","TermWindow.addch - point out of bounds: "+col+","+row);
		}	
	}

	private void advance() {
		col++;
		if (col >= cols) {
			row++;
			col = 0;
		}
		if (row >= rows) {
			row = rows-1;
		}
	}

	public void scroll() {
		for(int r=1;r<rows;r++) {
			for(int c=0;c<cols;c++) {
				buffer[r-1][c] = buffer[r][c];
			}
		}
	}
}