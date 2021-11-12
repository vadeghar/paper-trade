package com.algo.paper.trade.utils;

import java.awt.Toolkit;

public class CommonUtiils {
	
	public static void beep() {
		for(int i= 0; i<10;i++) {
			try {
				Toolkit.getDefaultToolkit().beep();
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

}
