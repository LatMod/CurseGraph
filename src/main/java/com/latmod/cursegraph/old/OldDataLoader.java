package com.latmod.cursegraph.old;

import com.google.gson.annotations.Expose;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OldDataLoader
{
	public static class OldGraphData
	{
		@Expose public Map<String, Map<Long, Integer>> projects;
	}
	
	public static void init() throws Exception
	{
        File oldDataFile = new File(CurseGraph.folder, "data.json");

        if(oldDataFile.exists())
		{
			OldGraphData ogd = Utils.fromJsonFile(oldDataFile, OldGraphData.class);
			
			if(ogd != null && ogd.projects != null)
			{
				for(String s : ogd.projects.keySet())
				{
					Map<Long, Integer> prj = ogd.projects.get(s);
					
					String[] s1 = new String[prj.size()];
					int idx = -1;
					
					for(Long l : prj.keySet())
						s1[++idx] = l.longValue() + ": " + prj.get(l).intValue();
					
					Arrays.sort(s1);

                    BufferedWriter bw = new BufferedWriter(new FileWriter(Utils.newFile(new File(CurseGraph.dataFolder, s + ".txt"))));
                    for(String s2 : s1) { bw.append(s2); bw.append('\n'); } bw.flush(); bw.close();
				}

                CurseGraph.info("Loaded old data!", true);
                oldDataFile.delete();
			}
		}

        File oldProjectsFile = new File(CurseGraph.folder, "projects.json");

        if(oldProjectsFile.exists())
		{
			List<String> l = Utils.fromJsonFile(oldProjectsFile, Utils.getListType(String.class));
			
			if(l != null && !l.isEmpty())
			{
                BufferedWriter bw = new BufferedWriter(new FileWriter(Utils.newFile(new File(CurseGraph.dataFolder, "projects.txt"))));

                for(String s : l)
				{
					String[] s1 = s.split("@", 2);
					
					if(s1 != null && s1.length == 2)
					{
						Curse.Type t = Curse.Type.get(s1[0]);
						if(t != null) bw.append(t.charID + ": " + s1[1] + "\n");
					}
				}
				
				bw.flush(); bw.close();
				oldProjectsFile.delete();
			}
		}
	}
}