//package com.tutorialspoint.xml;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Random;
public class TraceHVCTimestampingGargToken
{
    //static int highest_C_seensofar=0;
    static int snapshotcount=0;
    static String inpfilename="";
    static String outputLocation = "";
    static int debugmode=0;
    static int mode=0;
    public static void main(String[] args)
    {
        try
        {
            if(args.length < 4) {
                System.out.println("Expected number of arguments: 4. Provided "+args.length);
                System.exit(0);
            }
            debugmode = Integer.parseInt(args[0]);
            mode=Integer.parseInt(args[1]); //if 2-different-msg-distr-mode, anything else is normal msg distribution mode..
            /*
            if(mode==2) {
                System.out.println("Different message distribution mode");
            } else if(mode==1) {
                System.out.println("Intra group message distribution mode");
            } else {
                System.out.println("Normal message distribution mode");
            }*/
            inpfilename=args[2];
            outputLocation = args[3];
            File inputFile = new File(inpfilename);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            UserHandler userhandler = new UserHandler();
            saxParser.parse(inputFile, userhandler);
            System.out.println("The total snapshot count: "+snapshotcount);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
class UserHandler extends DefaultHandler
{
    boolean bmsender_time = false;
    boolean bmsgto = false;
    boolean bmsgfrom = false;
    boolean bmreceiver_time = false;
    boolean bstart_time=false;
    boolean bend_time=false;
    boolean bmisc=false;
    int proc_id=-1;//variable to remember process id
    int sender_time=-1;// variable to remember sender time for message RECEIVE
    int senderid=-1;// variable to remember sender id for message RECEIVE
    SysAtHand sysathand=new SysAtHand();
    Map<Integer, Process> mapofprocesses = new HashMap<Integer, Process>();//map of processes with process id as the key and Process instance as value
    Vector<Double> rcv_probab; //declared but will be defined only if in "different-msg-distr-mode"
    int previous_window=-1;
    Set<String> variableNameSet = new HashSet<String>();
    BufferedWriter bw1=null;
    BufferedWriter bw2=null;
    //setting new-folder's name using the input trace-file's name
    String folderName = TraceHVCTimestampingGargToken.inpfilename.substring(TraceHVCTimestampingGargToken.inpfilename.lastIndexOf('/')+1, TraceHVCTimestampingGargToken.inpfilename.lastIndexOf(".xml"));
    String nwfolder=TraceHVCTimestampingGargToken.outputLocation+"\\"+folderName; //input file name without file extension
    //file containing all hvc snapshots
    String snapshot_outfile=nwfolder+"\\snapshots_hvc_msgmode"+TraceHVCTimestampingGargToken.mode+".txt";
    //file containing only snapshots that were counted
    String snapshot_counted_outfile=nwfolder+"\\snapshots_counted_hvc"+TraceHVCTimestampingGargToken.mode+".txt";
    String tokens_file = nwfolder+"\\Tokens_hvc.txt";
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (qName.equalsIgnoreCase("message"))
        {
            String type = attributes.getValue("type");
            String process = attributes.getValue("process");
            //System.out.println("message " + type + " event at process " +process);
            proc_id=Integer.parseInt(process);
        }
        else if(qName.equalsIgnoreCase("sys"))
        {
            int eps = Integer.parseInt(attributes.getValue("epsilon"));
            int nproc = Integer.parseInt(attributes.getValue("number_of_processes"));
            //System.out.println("System: epsilon=" + eps + ", number_of_processes=" +nproc);
            sysathand.SetEpsilon(eps);
            sysathand.SetNumberOfProcesses(nproc);
            if((TraceHVCTimestampingGargToken.mode==1)||(TraceHVCTimestampingGargToken.mode==2))
            {
                rcv_probab=new Vector<Double>(nproc);
            }
            //create nproc number of instances of class process and assign ids to them
            for (int i=0; i<nproc; i++)
            {
                Vector<Integer> freshhvc=new Vector<Integer>(nproc);
                for (int m=0; m<nproc; m++)
                {
                    freshhvc.add(0);
                }
                Process proc = new Process(i,freshhvc,0);
                mapofprocesses.put(i,proc);
                if((TraceHVCTimestampingGargToken.mode==1)||(TraceHVCTimestampingGargToken.mode==2))
                {
                    if(i<nproc/2)
                    {
                        rcv_probab.add(0.5);
						/*
						if(i==0)
						{
							rcv_probab.add(0.10);
						}
						else if(i==1)
						{
							rcv_probab.add(0.20);
						}
						else if(i==2)
						{
							rcv_probab.add(0.30);
						}
						else if(i==3)
						{
							rcv_probab.add(0.40);
						}
						else
						{
							rcv_probab.add(0.5);
						}
						*/
                    }
                    else
                    {
                        rcv_probab.add(1.0);
                    }
                }
            }
        }
        else if (qName.equalsIgnoreCase("sender_time"))
        {
            bmsender_time = true;
        }
        else if (qName.equalsIgnoreCase("to"))
        {
            bmsgto = true;
        }
        else if (qName.equalsIgnoreCase("from"))
        {
            bmsgfrom = true;
        }
        else if (qName.equalsIgnoreCase("receiver_time"))
        {
            bmreceiver_time = true;
        }
        else if (qName.equalsIgnoreCase("interval"))
        {
            String process = attributes.getValue("process");
            //System.out.println("Interval at process " +process);
            proc_id=Integer.parseInt(process);
        }
        else if (qName.equalsIgnoreCase("start_time"))
        {
            bstart_time = true;
        }
        else if (qName.equalsIgnoreCase("end_time"))
        {
            bend_time = true;
        }
        else if (qName.equalsIgnoreCase("associated_variable"))
        {
            String name = attributes.getValue("name");
            String value = attributes.getValue("value");
            String old_value = attributes.getValue("old_value");
            if(value.equals("true"))
            {
                //System.out.println("true interval at "+proc_id);
                Process proc= mapofprocesses.get(proc_id);
                Vector<Integer> oldhvc=new Vector<Integer>(sysathand.GetNumberOfProcesses());
                Vector<Integer> currenthvc=new Vector<Integer>(sysathand.GetNumberOfProcesses());
                for(int huj=0;huj<sysathand.GetNumberOfProcesses();huj++)
                {
                    oldhvc.add(proc.getOldHvc().get(huj));
                }
                for(int pr=0;pr<sysathand.GetNumberOfProcesses();pr++)
                {
                    currenthvc.add(proc.getHvc().get(pr));
                }
                if(proc.getAcceptInterval()==0){
                    proc.newCandidateOccurance(oldhvc, currenthvc,proc.getOldPt(),proc.getPt());
                    mapofprocesses.put(proc_id,proc);
                    proc.setAcceptInterval(1);
                } else {
                    proc.setAcceptInterval(0);
                }
            }
        }
        else if (qName.equalsIgnoreCase("misc"))
        {
            bmisc = true;
        }
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if (qName.equalsIgnoreCase("message"))
        {
            //System.out.println("End Element :" + qName+ "\n");
        }
        else if(qName.equalsIgnoreCase("associated_variable"))
        {
            //System.out.println("End Element :" + qName);
        }
        else if(qName.equalsIgnoreCase("misc"))
        {
            //System.out.println("End Element :" + qName);
        }
        else if(qName.equalsIgnoreCase("interval"))
        {
        }
        else if(qName.equalsIgnoreCase("system_run"))
        {
            processHVCCandidates(mapofprocesses);
        }
    }
    @Override
    public void characters(char ch[], int start, int length) throws SAXException
    {
        if (bmsender_time)
        {
            sender_time=Integer.parseInt(new String(ch, start, length));
            //System.out.println("Sender time: "+ sender_time);
            bmsender_time = false;
        }
        else if (bmsgto)
        {
            int msgto=Integer.parseInt(new String(ch, start, length));
            //System.out.println("Message to: " + msgto);
            Process proc= mapofprocesses.get(proc_id);
            if(proc_id!=msgto)
            {
                proc.updateClock(sender_time,true,sysathand.GetNumberOfProcesses());
            }
            else
            {
                proc.updateClock(sender_time,false,sysathand.GetNumberOfProcesses());//no reporting required for intra-process communication, so logging corresponding l,c values in the queue is not required
            }
            mapofprocesses.put(proc_id,proc);
            proc_id=-1;
            sender_time=-1;
            //System.out.println("Clock updated after message send, l="+ proc.getL()+",c="+proc.getC());
            bmsgto = false;
        }
        else if (bmsgfrom)
        {
            senderid=Integer.parseInt(new String(ch, start, length));
            //System.out.println("Message from: " +senderid );
            bmsgfrom = false;
        }
        else if (bmreceiver_time)
        {
            int receiver_time=Integer.parseInt(new String(ch, start, length));
            //System.out.println("Receiver time: " + receiver_time);
            //get max of sendertime,receiver_time
            //update clock using that max
            Process proc= mapofprocesses.get(proc_id);
            boolean toss;
            if((TraceHVCTimestampingGargToken.mode==1) && ((proc_id<5 && senderid>=5)||(proc_id>=5 && senderid<5)))//cross group communication in the case of mode 1
            {
                toss=false;
            }
            else if((TraceHVCTimestampingGargToken.mode==2) || (TraceHVCTimestampingGargToken.mode==1))// intra group communication in mode 1 OR mode 2
            {
                //System.out.println("rcv_probab at p"+proc_id+" : "+rcv_probab.get(proc_id));
                int rangeend=(int) (1/rcv_probab.get(proc_id)); //2 if probab is 0.5, and 1 otherwise
                toss= new Random().nextInt(rangeend)==0; //
            }
            else
            {
                toss=true; // every process receives every message from any other process
            }
            if((proc_id!=senderid) && (toss))//based on senderid and on receiver-probability--- if in different msg distribution mode
            {
                //get sender l,c by popping sender's dequeue
                Process senderproc= mapofprocesses.get(senderid);
                MessageSendStruct correspSendHVC = senderproc.getHVCfromQueue(sender_time);
                Vector<Integer> currenthvc=new Vector<Integer>(sysathand.GetNumberOfProcesses());
                if(proc.getlastsendorrecorlocevntpt()!=receiver_time)//if a message send/receive did not happen at the same instant update old pt - otherwise don't because old pt is required for interval reporting
                {
                    for(int prId=0;prId<sysathand.GetNumberOfProcesses();prId++)
                    {
                        //currenthvc.set(huj,proc.getHvc().get(huj));
                        currenthvc.add(proc.getHvc().get(prId));
                    }
                    proc.setOldHvc(currenthvc);
                    proc.setOldPt(proc.getPt());
                }
                Vector<Integer> updatedhvc=new Vector<Integer>(sysathand.GetNumberOfProcesses());//need separate vectors because they behave like objects
                for(int pr=0; pr<sysathand.GetNumberOfProcesses();pr++)
                {
                    if(pr==proc_id)
                    {
                        //updatedhvc.set(id,(updatedhvc.get(id))+1);
                        updatedhvc.add((proc.getHvc().get(proc_id))+1);
                    } else {
                        updatedhvc.add(Math.max(correspSendHVC.getHvc().get(pr),proc.getHvc().get(pr)));
                    }
                }
                proc.setHvc(updatedhvc);
                proc.setPt(receiver_time);
                proc.setlastsendorrecorlocevntpt(receiver_time);
                mapofprocesses.put(proc_id,proc);//update the process instance in the map corresponding the key-process id
            }
            else
            {
                if(proc_id!=senderid) // case where it chose to ignore msg based on probability OR due to cross group communication in the case of mode 1
                {
                    // to pop corresponding sender info from its queue
                    Process senderproc= mapofprocesses.get(senderid);//get sender hvc by popping sender's dequeue
                    MessageSendStruct correspSendHVC = senderproc.getHVCfromQueue(sender_time); //pop it but ignore it
                    mapofprocesses.put(senderid,senderproc);
                }
                proc.updateClock(receiver_time,false,sysathand.GetNumberOfProcesses());//treat like local event if toss is false
                mapofprocesses.put(proc_id,proc);//update the process instance in the map corresponding the key-process id
            }
            bmreceiver_time = false;
            proc_id=-1;
            sender_time=-1;
            senderid=-1;
        }
        else if (bstart_time)
        {
            //System.out.println("Interval start time: "+ new String(ch, start, length));
            bstart_time = false;
        }
        else if (bend_time)
        {
            int end_time=Integer.parseInt(new String(ch, start, length));
            //System.out.println("Interval end time: " + end_time);
            Process proc= mapofprocesses.get(proc_id);
            //no need to update clocks if bmisc because the clock was already updated at message send/recieve which actually caused this interval end point
            //if(!bmisc)//uncommented to create a new timestamp for the next interval's start - to allow choosing the next interval as part of valid snapshot
            {
                proc.updateClock(end_time,false,sysathand.GetNumberOfProcesses());
                mapofprocesses.put(proc_id,proc);
            }
            bmisc = false;
            bend_time = false;
        }
        else if (bmisc)
        {
            //System.out.println("misc: " + new String(ch, start, length));
        }
    }
    void processHVCCandidates(Map<Integer, Process> mapofprocesses){
        //create needed parent directory/clean necessary output files
        filePrep(snapshot_outfile);
        filePrep(snapshot_counted_outfile);
        filePrep(tokens_file);
        Token token=new Token(sysathand.GetNumberOfProcesses());
        token.setTokenOwner(0);
        boolean noMoreCandidates=false;
        //until you run out of candidates for some process
        while (!noMoreCandidates) {
            int tokOwner = token.getTokenOwner();
            //check if token owner has a valid non-dummy candidate representative -- first iteration of the loop
            if (!token.representativeIsSetAt(tokOwner)){
                Process proc = mapofprocesses.get(tokOwner);
                Candidate nextCand = proc.getFirstCandidate();
                mapofprocesses.put(tokOwner,proc);//update map to reflect the change done to the process
                if (nextCand != null) {
                    //sets the next candidate as representative
                    token.representativeSetCandidateAt(tokOwner, nextCand);
                } else { //queue is empty
                    noMoreCandidates = true;
                    //System.out.println("No more candidates at "+tokOwner);
                    continue;
                }
            }
            //go ahead and evaluate token only if it is complete
            int passItOnTo = token.isIncompleteAt();
            if (passItOnTo != -1) { //indicates token is complete
                token.setTokenOwner(passItOnTo);
                continue;
            }
            //if token if complete
            //evaluate if representative of the current token-owner-process is concurrent with all others
            boolean valid = token.computeIfValidCandidate(tokOwner, sysathand.GetEpsilon());
            //if color is still red for the token-owner's representative - in this case ComputeIfOverlap would
            //have returned false without changing the token owner--so set the next candidate in the queue of
            //the owner process as the new representative and continue the while loop i.e. evaluate the token
            //with the new representative in the next iteration of the loop
            if(!valid && tokOwner == token.ownerId){
                Process proc = mapofprocesses.get(tokOwner);
                Candidate nextCand = proc.getFirstCandidate();
                mapofprocesses.put(tokOwner,proc);//update map to reflect the change done to the process
                if (nextCand != null) {
                    //sets the next candidate as representative
                    token.representativeSetCandidateAt(tokOwner, nextCand);
                } else { //queue is empty
                    noMoreCandidates = true;
                    System.out.println("No more candidates at "+tokOwner);
                    continue;
                }
            } else if (!valid && tokOwner != token.ownerId) {
                //valid was set to false due to an invalid candidate representative for a non-token-owner process
                //owner was already set to that process in computeIfValidCandidate
                //pass on the token to that process
            } else if (valid && token.getFirstRedProcess()!=-1) {
                //current candidate representative for the token process is valid but the token evaluation is
                //not complete yet - should evaluate every other process' candidate
                token.setTokenOwner(token.getFirstRedProcess());//pass on the token to appropriate owner
            } else {//token was evaluated for every pair of candidates and is completely green
                if (TraceHVCTimestampingGargToken.debugmode == 1) {
                    token.markAs(tokens_file, "Accepted");
                }
                //printing it to all snapshots file
                token.print(snapshot_outfile);
                //counting snapshots only if they are more than epsilon apart from each other
                int temp_wind = previous_window;
                previous_window = count(token, previous_window);
                if (temp_wind != previous_window) {
                    //System.out.println("New window:"+previous_window);
                    //printed to counted-snapshots file
                    token.markAs(snapshot_counted_outfile," Snapshot No:"+TraceHVCTimestampingGargToken.snapshotcount+"-->");
                    token.print(snapshot_counted_outfile);
                    token.markAs(snapshot_outfile, " Was Counted");
                    //clear token
                    token = new Token(sysathand.GetNumberOfProcesses());//default owner is process 0
                } else {
                    //make the process with the smallest start pt red in the token
                    int newOwner = token.computesmalleststart().get(1);
                    token.clearCandidateAt(newOwner,sysathand.GetNumberOfProcesses());
                    token.setTokenOwner(newOwner);//returns the process with smallest start PT
                }
            }
        }
    }
    int count(Token token, int previous_window){
        //compute the current cut's window based on epsilon
        int current_cut_window = token.getWindow(sysathand.GetEpsilon());
        if ((TraceHVCTimestampingGargToken.snapshotcount == 0) || (current_cut_window > previous_window)) {
            TraceHVCTimestampingGargToken.snapshotcount++;
            previous_window = current_cut_window;
        }
        return previous_window;
    }
    void filePrep(String filename){
        try {
            File ifilename = new File(filename);
            ifilename.getParentFile().mkdirs(); //create all necessary parent directories
            bw1= new BufferedWriter(new FileWriter(ifilename));//will cause the file-cleanup to start with-because you are opening file in default write mode instead of append
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}