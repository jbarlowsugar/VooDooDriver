/*
Copyright 2011 SugarCRM Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. 
You may may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0 
   
Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
Please see the License for the specific language governing permissions and 
limitations under the License.
*/

package voodoodriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SodaOSInfo {

	public SodaOSInfo() {
		
	}
	
	public static SodaSupportedOS getOS() {
		SodaSupportedOS type = null;
		String value = "";
		
		value = System.getProperty("os.name").toLowerCase();
		if (value.contains("win")) {
			type = SodaSupportedOS.WINDOWS;
		} else if (value.contains("linux")) {
			type = SodaSupportedOS.LINUX;
		} else if (value.contains("mac")) {
			type = SodaSupportedOS.OSX;
		} else {
			type = null;
		}
		
		return type;
	}
	
	private static ArrayList<Integer> getUnixPids(String process) {
		ArrayList<Integer> pids = new ArrayList<Integer>();
		Process proc = null;
		BufferedReader reader;
		String[] cmd = {"ps", "x", "-o", "pid,comm"};
		
		try {
			proc = Runtime.getRuntime().exec(cmd);
			reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			Pattern p = Pattern.compile(String.format("(^\\d+)\\s+(.*%s.*)", process));
			Matcher m = null;
			
			while ((line = reader.readLine()) != null) {
				line = line.replaceAll("^\\s+", "");
				if (line.contains("PID COMMAND") || line.contains("PID COMM")) {
					continue;
				}
				
				m = p.matcher(line);
				if (m.find()) {
					String pid = m.group(1);
					pids.add(Integer.valueOf(pid));
				}
			}
		} catch(Exception exp) {
			exp.printStackTrace();
			pids = null;
		}
		
		return pids;
	}
	
	private static ArrayList<Integer> getWindowsPids(String process) {
		ArrayList<Integer> pids = new ArrayList<Integer>();
		String[] cmd = {"tasklist.exe", "/FO", "CSV", "/NH"};
		Process proc = null;
		BufferedReader reader = null;
		
		try {
			proc = Runtime.getRuntime().exec(cmd);
			reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;

			while ((line = reader.readLine()) != null) {
				String data[] = line.split(",");
				String proc_name = data[0];
				String pid = data[1];
				
				proc_name = proc_name.replaceAll("^\"", "");
				proc_name = proc_name.replaceAll("\"$", "");
				pid = pid.replaceAll("^\"", "");
				pid = pid.replaceAll("\"$", "");
				if (proc_name.contains(process)) {
					pids.add(Integer.valueOf(pid));
				}
			}
		} catch (Exception exp) {
			exp.printStackTrace();
			pids = null;
		}

		return pids;
	}
	
	public static ArrayList<Integer> getProcessIDs(String process) {
		ArrayList<Integer> pids = null;
		SodaSupportedOS os = getOS();
		
		switch(os) {
		case OSX:
			pids = getUnixPids(process);	
			break;
		case WINDOWS:
			pids = getWindowsPids(process);
				break;
		case LINUX:
			pids = getUnixPids(process);
			break;
		}
		
		return pids;
	}
	
	private static boolean killWindowsProcess(Integer pid) {
		boolean result = false;
		String[] cmd = {"taskkill.exe", "/T", "/F", "/PID", pid.toString()};
		Process proc = null;
		int ret = 0;
		
		try {
			proc = Runtime.getRuntime().exec(cmd);
			Thread.sleep(5000); // this is to bypass a windows wait issue... //
			ret = proc.exitValue();
			if (ret != 0) {
				System.out.printf("(!)Error: Failed trying to kill process by pid: '%d'\n", pid);
				result = false;
			} else {
				System.out.printf("(*)Killed process by pid: '%d'.\n", pid);
				result = true;
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		return result;
	}
	
	private static boolean killUnixProcess(Integer pid) {
		boolean result = false;
		String[] cmd = {"kill", "-9", pid.toString()};
		Process proc = null;
		int ret = 0;
		
		try {
			proc = Runtime.getRuntime().exec(cmd);
			Thread.sleep(5000);
			ret = proc.exitValue();
			if (ret != 0) {
				System.out.printf("(!)Error: Failed trying to kill process by pid: '%d'\n", pid);
				result = false;
			} else {
				System.out.printf("(*)Killed process by pid: '%d'.\n", pid);
				result = true;
			}
		} catch (Exception exp) {
			exp.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean killProcesses(ArrayList<Integer> list) {
		boolean result = false;
		boolean err = false;
		SodaSupportedOS os = getOS();
		int len = list.size() -1;
		
		for (int i = 0; i <= len; i++) {
			int pid = list.get(i);
			switch(os) {
			case OSX:
				err = killUnixProcess(pid);
				break;
			case WINDOWS:
				err = killWindowsProcess(pid);
				break;
			case LINUX:
				err = killUnixProcess(pid);
				break;
			}
			
			if (err != true) {
				System.out.printf("(!)Failed to kill pid: '%s'!\n", pid);
				result = false;
			}
		}
		
		return result;
	}	
}
