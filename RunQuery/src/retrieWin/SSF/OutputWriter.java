package retrieWin.SSF;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import fig.basic.LogInfo;

public class OutputWriter {
	BufferedWriter writer;
	String POCEmail = "ajuts@stanford.edu", TeamID = "RetrieWin", TeamName = "Stanford University", POCName = "Aju T Scaria", SystemID = "Submission_1";
	public OutputWriter(String fileName){
		try {
			writer = new BufferedWriter(new FileWriter(fileName));
			
			writer.write(String.format("#{\"run_type\": \"automatic\", \"poc_email\": \"%s\", \"team_id\": \"%s\", \"topic_set_id\": \"sample-trec-kba-topics-2013\", \"corpus_id\": \"tiny-corpus\", \"$schema\": \"http://trec-kba.org/schemas/v1.1/filter-run.json\", \"team_name\": \"%s\", \"system_description_short\": \"rating=2,contains_mention=1,confidence=exact_match_name_tokens/num_name_tokens\", \"system_description\": \"Collapses entity title strings and documents into sets of words and looks for fraction of exact match overlap with entity titles.  Confidence is fraction of entity title words that appear in doc.\", \"task_id\": \"kba-ssl-2013\", \"poc_name\": \"%s\", \"run_info\": {\"num_entities\": 4, \"num_stream_hours\": 3}, \"system_id\": \"%s\"}\n",
									POCEmail, TeamID, TeamName, POCName, SystemID));
		} catch (IOException e) {
			LogInfo.logs(e);
			e.printStackTrace();
		}
	}
	/*
	 * streamID: Stream ID from the 
	 * entityID: Entity
	 */
	public void Write(String streamID, String entityID, double confidenceScore, String directoryName, String slotName, String equivalenceClass, Long long1, Long long2) {
		try {
			writer.write(String.format("%s\t%s\t%s\t%s\t%f\t%s\t%s\t%s\t%s\t%s\t%s\n",
					TeamID, SystemID, streamID, entityID, confidenceScore,"2", "1", directoryName, slotName, equivalenceClass, (long1 + "-"  + long2)));
		} catch (IOException e) {
			LogInfo.logs("Exception caught: " + e);
			e.printStackTrace();
		}
	}
	
	public void Close() {
		try {
			writer.write(String.format("#{\n" + 
							"#    \"run_type\": \"automatic\", \n" +
							"#    \"poc_email\": \"%s\",\n" +
							"#    \"team_id\": \"%s\",\n" +
							"#    \"topic_set_id\": \"sample-trec-kba-topics-2013\",\n" + 
							"#    \"corpus_id\": \"tiny-corpus\", \n" +
							"#    \"$schema\": \"http://trec-kba.org/schemas/v1.1/filter-run.json\", \n" +  
							"#    \"team_name\": \"%s\", \n"+
							"#    \"system_description_short\": \"rating=2,contains_mention=1,confidence=exact_match_name_tokens/num_name_tokens\",\n" + 
							"#    \"system_description\": \"Collapses entity title strings and documents into sets of words and looks for fraction of exact match overlap with entity titles.  Confidence is fraction of entity title words that appear in doc.\",\n"+ 
							"#    \"task_id\": \"kba-ccr-2012\", \n"+
							"#    \"poc_name\": \"%s\",\n"+ 
							"#    \"run_info\": {\n"+
							"#        \"num_filter_results\": 118,\n"+ 
							"#        \"elapsed_time\": 52.14945411682129,\n"+ 
							"#        \"num_entities\": 4, \n"+
							"#        \"num_entity_doc_compares\": 3312,\n"+ 
							"#        \"num_stream_hours\": 3\n"+
							"#    }, \n"+
							"#    \"system_id\": \"%s\"\n"+
							"#}", POCEmail, TeamID, TeamName, POCName, SystemID));
			writer.close();
		} catch (IOException e) {
			LogInfo.logs("Exception caught: " + e);
			e.printStackTrace();
		}
	}
}
