package net.fs.utils;

import java.io.OutputStream;

public interface LogListener {
	
	public void onAppendContent(OutputStream los,String text);
	
}
