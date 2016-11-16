package com.latmod.cursegraph.old;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class Graph
{
	public static class GraphData
	{
		public final String projectID;
		public final ArrayList<TimedDown> downloads;
		
		public GraphData(String s)
		{
			projectID = s;
			downloads = new ArrayList<TimedDown>();
		}
		
		public String toString()
		{ return projectID; }
		
		public boolean equals(Object o)
		{
			if(o == this) return true;
			return o.toString().equals(toString());
		}
		
		public void refresh()
		{ allDataMap.put(projectID, this); }

		public int lastDownloads()
		{
			int d = -1;
			for(TimedDown td : downloads)
				if(td.down > d) d = td.down;
			return d;
		}
	}
	
	public static class Checker implements Runnable
	{
		public Thread thread = null;
		
		public void start()
		{
			if(thread == null)
			{
				thread = new Thread(this);
				thread.setDaemon(true);
			}
		}
		
		public void run()
		{
			try
			{
				boolean first = true;
				long lastMS = 0L;
				
				while(true)
				{
					long ms = System.currentTimeMillis();

                    if(first)
                    {
                        first = false;
                        lastMS = ms;
                    }
                    else
                    {
                        CurseGraph.refresh();
                    }

                    long reqMS = CurseGraph.config.refreshMinutes.intValue() * 60L * 1000L;

                    while(ms - lastMS < reqMS);
					lastMS = ms;
					logData();
				}
			}
			catch(Exception e)
			{ e.printStackTrace(); thread = null; }
		}
	}
	
	public static class TimedDown implements Comparable<TimedDown>
	{
		public long time;
		public int down;
		
		public TimedDown(long t, int v)
		{ time = t; down = v; }
		
		public int compareTo(TimedDown o)
		{ return (time < o.time ? -1 : (time == o.time ? 0 : 1)); }
	}
	
	public static final HashMap<String, GraphData> allDataMap = new HashMap<String, GraphData>();
	public static final Checker checker = new Checker();
	
	public static void init() throws Exception
	{
		for(Curse.Project p : Projects.list)
		{
			GraphData data = getData(p.projectID);
			data.downloads.clear();

            File f = new File(CurseGraph.config.dataFolderPath, p.projectID + ".txt");

            if(f.exists())
			{
				BufferedReader br = new BufferedReader(new FileReader(f));
				String s = null;
				
				while((s = br.readLine()) != null)
				{
					String[] s2 = s.split(": ", 2);
					
					if(s2 != null && s2.length == 2)
					{
						long t = Long.parseLong(s2[0]);
						int d = Integer.parseInt(s2[1]);
						data.downloads.add(new Graph.TimedDown(t, d));
					}
				}
				
				br.close();
			}
		}
		
		saveGraph();
		checker.start();
	}
	
	public static void saveGraph() throws Exception
	{
        int exportW = CurseGraph.config.exportGraph[0];
        int exportH = CurseGraph.config.exportGraph[1];

        for(Curse.Project p : Projects.list)
		{
			GraphData data = getData(p.projectID);
			Graph.TimedDown[] downs = data.downloads.toArray(new Graph.TimedDown[0]);
			
			if(downs.length > 0)
			{
                BufferedWriter bw = new BufferedWriter(new FileWriter(Utils.newFile(new File(CurseGraph.config.dataFolderPath, p.projectID + ".txt"))));
                Arrays.sort(downs); for(Graph.TimedDown t : downs) bw.append(t.time + ": " + t.down + "\n"); bw.flush(); bw.close();
				
				if(exportW > 0 && exportH > 0)
				{
					try
					{
						JCurseGraph.mouse = false;
						JCurseGraph j = new JCurseGraph(p);
						j.setSize(exportW, exportH);
						BufferedImage image = new BufferedImage(exportW, exportH, BufferedImage.TYPE_INT_RGB);
						j.paint(image.getGraphics());
                        ImageIO.write(image, "PNG", Utils.newFile(new File(CurseGraph.dataFolder, p.projectID + ".png")));
                    }
					catch(Exception e)
					{ e.printStackTrace(); }
					JCurseGraph.mouse = true;
				}
			}
		}
		
		System.gc();
	}
	
	@SuppressWarnings("deprecation")
	public static String getTimeString(long time)
	{
		Date date = new Date(time);
		String s = "";
		s += formNum(date.getDate());
		s += ".";
		s += formNum(date.getMonth() + 1);
		s += ".";
		s += formNum(date.getYear() + 1900);
		s += " ";
		s += formNum(date.getHours());
		s += ":";
		s += formNum(date.getMinutes());
		s += ":";
		s += formNum(date.getSeconds());
		return s;
	}
	
	private static String formNum(int i)
	{ return (i < 10) ? ("0" + i) : ("" + i); }
	
	public static GraphData getData(String s)
	{
		GraphData d = allDataMap.get(s);
		if(d == null) { d = new GraphData(s);
		allDataMap.put(s, d); } return d;
	}
	
	public static ArrayList<TimedDown> getDownloads(String s)
	{
		ArrayList<TimedDown> alist = new ArrayList<TimedDown>();
		GraphData data = getData(s);
		if(data == null || data.downloads.isEmpty()) return alist;
		alist.addAll(data.downloads);
		return alist;
	}
	
	public static ArrayList<TimedDown> getDownloads(String s, long min, long max)
	{
		ArrayList<TimedDown> al = getDownloads(s);
		
		for(int i = 0; i < al.size(); i++)
		{
			long v = al.get(i).time;
			if(v < min && v > max)
				al.remove(i);
		}
		
		return al;
	}
	
	public static void logData() throws Exception
	{
		long ms = System.currentTimeMillis();
		
		if(Projects.hasProjects()) for(Curse.Project p : Projects.list)
		{
			GraphData data = getData(p.projectID);
			
			int d = p.getTotalDownloads();
			int ld = data.lastDownloads();
			
			if(d > ld) data.downloads.add(new TimedDown(ms, d));
		}
		
		saveGraph();
	}
	
	public static int clearData(long l)
	{
		int i = 0;
		for(Curse.Project p : Projects.list)
			i += clearData(p.projectID, l);
		return i;
	}
	
	public static int clearData(String s, long l)
	{
		long ms = System.currentTimeMillis();
		
		GraphData data = getData(s);
		
		ArrayList<TimedDown> newList = new ArrayList<TimedDown>();
		
		for(int i = 0; i < data.downloads.size(); i++)
		{
			TimedDown t = data.downloads.get(i);
			if((ms - t.time) < l) newList.add(t);
		}
		
		int rem = data.downloads.size() - newList.size();
		data.downloads.clear();
		data.downloads.addAll(newList);
		return rem;
	}
}