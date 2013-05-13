package retrieWin.PatternBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrieWin.Indexer.ProcessTrecTextDocument;
import retrieWin.Indexer.TrecTextDocument;
import retrieWin.Querying.ExecuteQuery;
import retrieWin.Querying.QueryBuilder;
import retrieWin.SSF.SlotPattern;
import retrieWin.Utils.NLPUtils;

import edu.stanford.nlp.util.Pair;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class Aju implements Runnable{
	@Option(gloss="working Directory") public String workingDirectory;
	@Option(gloss="index Location") public String indexLocation;
	
	public static void main(String[] args) {
		Execution.run(args, "Main", new Aju());
	}

	@Override
	public void run() {
		LogInfo.begin_track("run()");
		List<Pair<String,String>> bootstrapList = getBootstrapInput("src/seedSet/slot_Founded_by");
		NLPUtils utils = new NLPUtils();
		for(Pair<String, String> pair:bootstrapList) {
			ExecuteQuery eq = new ExecuteQuery(indexLocation);
			List<TrecTextDocument> trecDocs = eq.executeQuery(QueryBuilder.buildUnorderedQuery(pair.first, pair.second, 10), 100, workingDirectory);
			
			for(String str:ProcessTrecTextDocument.extractRelevantSentences(trecDocs, pair.first, pair.second)) {
				LogInfo.logs(str);
				List<SlotPattern> patterns = utils.findSlotPattern(str, pair.first, pair.second);
				for(SlotPattern pattern:patterns)
					LogInfo.logs(pattern);
				
			}
		}
		LogInfo.end_track();
	}
	
	private static List<Pair<String, String>> getBootstrapInput(String fileName) {
		List<Pair<String, String>> bootstrapList = new ArrayList<Pair<String, String>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String line = null;
			try {
				while((line=reader.readLine())!=null) {
					String[] splits = line.split("\t");
					bootstrapList.add(new Pair<String, String>(splits[0], splits[1]));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bootstrapList;
	}
}
