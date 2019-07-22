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
    static int debugmode=0;
    static int mode=0;
    public static void main(String[] args)
    {
        try
        {
            debugmode = Integer.parseInt(args[0]);
            mode=Integer.parseInt(args[1]); //if 2-different-msg-distr-mode, anything else is normal msg distribution mode..
            if(mode==2)
            {
                System.out.println("Different message distribution mode");
            }
            else if(mode==1)
            {
                System.out.println("Intra group message distribution mode");
            }
            else
            {
                System.out.println("Normal message distribution mode");
            }
            //File inputFile = new File("../print_traces_forpredicate_detection_xml_singleboolvariable_x/predicate_a0.010000_e10_l0.100000_d100_v10_run0.xml");
            //inpfilename="predicate_a0.010000_e1000_l0.010000_d100_v10_run0_100000runs_1.xml";
            inpfilename="../first2000_predicate_a0.010000_e100_l0.100000_d10_v1_run0.xml";
            //inpfilename="predicate_a0.100000_e100_l1.000000_d10_v1_run0.xml";
            //File inputFile = new File("predicate_a0.010000_e1000_l0.010000_d100_v10_run0_100000runs_1.xml");
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

    Token token;

    Vector<Double> rcv_probab; //declared but will be defined only if in "different-msg-distr-mode"

    int previous_window=0;

    Set<String> variableNameSet = new HashSet<String>();

    //variables for printing z3 constraints for intervals
    String intervalConstraint="";
    int bracescount=0;

    BufferedWriter bw1=null;
    BufferedWriter bw2=null;
    String nwfolder=TraceHVCTimestampingGargToken.inpfilename.substring(0, TraceHVCTimestampingGargToken.inpfilename.lastIndexOf('.')); //input file name without file extension
    String snapshot_outfile=nwfolder+"\\snapshots_hvc_msgmode"+TraceHVCTimestampingGargToken.mode+".txt";
    String snapshot_counted_outfile=nwfolder+"\\snapshots_counted_hvc"+TraceHVCTimestampingGargToken.mode+".txt";

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
            token=new Token(nproc);
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
                try {
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
                    token=proc.newCandidateOccurance(token, oldhvc, currenthvc,proc.getOldPt(),proc.getPt(),true,sysathand.GetEpsilon());
                    mapofprocesses.put(proc_id,proc);
                    if((token.getCandidateAt(proc_id).getcolor()=="green") && (token.getTokenOwner()==proc_id))//token is green at procid
                    {
                        int k=0;
                        boolean found=true;
                        while(k<sysathand.GetNumberOfProcesses())//loop through token
                        {
                            if(k!=proc_id)
                            {
                                if(token.getCandidateAt(k).getcolor()=="red")
                                {
                                    //pass token to a process with red color i.e. set token owner id to that process
                                    token.setTokenOwner(k);
                                    k=sysathand.GetNumberOfProcesses();
                                    found=false;
                                }
                            }
                            k++;
                        }
                        //if all other processes have green color then
                        if(found)
                        {
                            //report detection
                            //System.out.println("Predicate Satisfied");
                            if(TraceHVCTimestampingGargToken.snapshotcount==0)//for first predicate satisfaction detected-clear snapshot file if one exists
                            {
                                //Creating all necessary files
                                File ifilename = new File(snapshot_outfile);
                                ifilename.getParentFile().mkdirs(); //create all necessary parent directories
                                bw1= new BufferedWriter(new FileWriter(ifilename));//will cause the file-cleanup to start with-because you are opening file in default write mode instead of append
                                File ifilename1 = new File(snapshot_counted_outfile);
                                ifilename1.getParentFile().mkdirs(); //create all necessary parent directories
                                bw2= new BufferedWriter(new FileWriter(ifilename1));//will cause the file-cleanup to start with-because you are opening file in default write mode instead of append
                            }
                            //report overlap
                            if(TraceHVCTimestampingGargToken.debugmode==1)
                            {
                                //JUST PRINTING FOR DEBUGGING
                                BufferedWriter candbw1= new BufferedWriter(new FileWriter("Tokens_hvc.txt", true));//true for append
                                candbw1.append("Accepted.\n");
                                candbw1.close();
                            }
                            boolean markifcounted=false;
                            //compute the current cut's window based on epsilon
                            int current_cut_window=token.getWindow(sysathand.GetEpsilon());
                            if((TraceHVCTimestampingGargToken.snapshotcount==0)||(current_cut_window>previous_window))
                            {
                                TraceHVCTimestampingGargToken.snapshotcount++;
                                previous_window=current_cut_window;

                                markifcounted=true;
                            }
                            if(markifcounted)
                            {
                                bw2= new BufferedWriter(new FileWriter(snapshot_counted_outfile, true));//true for append
                                bw2.write("At Process"+proc_id+" Snapshot No:"+TraceHVCTimestampingGargToken.snapshotcount+"-->");
                                for(int i=0;i<sysathand.GetNumberOfProcesses();i++)//loop through token entries
                                {
                                    Candidate tempCand4=token.getCandidateAt(i);
                                    Vector<Integer> sthvc=tempCand4.getstart_hvc();
                                    Vector<Integer> endhvc=tempCand4.getend_hvc();
                                    bw2.write("[P"+i+":<");
                                    for(int b=0;b<sthvc.size();b++)
                                    {
                                        bw2.write(sthvc.get(b)+",");
                                    }
                                    bw2.write("> - <");
                                    for(int b=0;b<endhvc.size();b++)
                                    {
                                        bw2.write(endhvc.get(b)+",");
                                    }
                                    bw2.write("><pt:"+tempCand4.getstart_pt()+" - "+tempCand4.getend_pt()+">]\n");
                                }
                                //bw2.write("\n");
                                bw2.newLine();
                            }
                            //writing to all-snapshot file
                            bw1= new BufferedWriter(new FileWriter(snapshot_outfile, true));//true for append
                            bw1.write("At Process"+proc_id+" Snapshot No:"+TraceHVCTimestampingGargToken.snapshotcount+"-->");
                            for(int i=0;i<sysathand.GetNumberOfProcesses();i++)//loop through token entries
                            {
                                Candidate tempCand4=token.getCandidateAt(i);
                                Vector<Integer> sthvc=tempCand4.getstart_hvc();
                                Vector<Integer> endhvc=tempCand4.getend_hvc();
                                bw1.write("[P"+i+":<");
                                for(int b=0;b<sthvc.size();b++)
                                {
                                    bw1.write(sthvc.get(b)+",");
                                }
                                bw1.write("> - <");
                                for(int b=0;b<endhvc.size();b++)
                                {
                                    bw1.write(endhvc.get(b)+",");
                                }
                                bw1.write("><pt:"+tempCand4.getstart_pt()+" - "+tempCand4.getend_pt()+">]\n");
                            }
                            if(markifcounted)
                            {
                                bw1.write(" Was Counted");
                                markifcounted=false;
                            }
                            //bw1.write("\n");
                            bw1.newLine();
                            //clear token
                            token=new Token(sysathand.GetNumberOfProcesses());
                        }//end of found/reported pred satisfaction
                    }
                    else//not token owner-token was handed over to another process (withing missing representative Or red candidate representative) or I(procid) don't have the right candidate yet-red
                    {//keep token at the current owner
                    }
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                finally {
                    // always close the file
                    if (bw1 != null)
                        try {
                            bw1.close();
                        }
                        catch (IOException ioe1) {
                            ioe1.printStackTrace();
                        }
                    if (bw2 != null)
                        try {
                            bw2.close();
                        }
                        catch (IOException ioe2) {
                            ioe2.printStackTrace();
                        }
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
            //until you run out of candidates for some process - let say process 0
            int proc_id=0;
            boolean nomorecand=false;
            try {
                while (!nomorecand) {
                    Process proc = mapofprocesses.get(proc_id);
                    Vector<Integer> oldhvc = new Vector<Integer>(sysathand.GetNumberOfProcesses());
                    Vector<Integer> currenthvc = new Vector<Integer>(sysathand.GetNumberOfProcesses());
                    for (int j = 0; j < sysathand.GetNumberOfProcesses(); j++) {
                        oldhvc.add(proc.getOldHvc().get(j));
                    }
                    for (int pr = 0; pr < sysathand.GetNumberOfProcesses(); pr++) {
                        currenthvc.add(proc.getHvc().get(pr));
                    }
                    //just invoking function to handle remaining unprocessed intervals
                    token = proc.newCandidateOccurance(token, oldhvc, currenthvc, proc.getOldPt(), proc.getPt(), false, sysathand.GetEpsilon());//but don't push a candidate-pop and set a candidate if no current representative
                    mapofprocesses.put(proc_id, proc);
                    if ((token.getCandidateAt(proc_id).getcolor() == "green") && (token.getTokenOwner() == proc_id))//token is green at procid
                    {
                        int k = 0;
                        boolean found = true;
                        while (k < sysathand.GetNumberOfProcesses())//loop through token
                        {
                            if (k != proc_id) {
                                if (token.getCandidateAt(k).getcolor() == "red") {
                                    //pass token to a process with red color i.e. set token owner id to that process
                                    token.setTokenOwner(k);
                                    k = sysathand.GetNumberOfProcesses();
                                    found = false;
                                }
                            }
                            k++;
                        }
                        //if all other processes have green color then
                        if (found) {
                            //report detection
                            //System.out.println("Predicate Satisfied");
                            //report overlap
                            if (TraceHVCTimestampingGargToken.debugmode == 1) {
                                //JUST PRINTING FOR DEBUGGING
                                BufferedWriter candbw1 = new BufferedWriter(new FileWriter("Tokens_hvc.txt", true));//true for append
                                candbw1.append("Accepted.\n");
                                candbw1.close();
                            }
                            boolean markifcounted = false;
                            //compute the current cut's window based on epsilon
                            int current_cut_window = token.getWindow(sysathand.GetEpsilon());
                            if ((TraceHVCTimestampingGargToken.snapshotcount == 0) || (current_cut_window > previous_window)) {
                                TraceHVCTimestampingGargToken.snapshotcount++;
                                previous_window = current_cut_window;

                                markifcounted = true;
                            }
                            bw1 = new BufferedWriter(new FileWriter(snapshot_outfile, true));//true for append
                            bw1.write("At Process" + proc_id + " Snapshot No:" + TraceHVCTimestampingGargToken.snapshotcount + "-->");
                            for (int i = 0; i < sysathand.GetNumberOfProcesses(); i++)//loop through token entries
                            {
                                Candidate tempCand4 = token.getCandidateAt(i);
                                Vector<Integer> sthvc = tempCand4.getstart_hvc();
                                Vector<Integer> endhvc = tempCand4.getend_hvc();
                                bw1.write("[P" + i + ":<");
                                for (int b = 0; b < sthvc.size(); b++) {
                                    bw1.write(sthvc.get(b) + ",");
                                }
                                bw1.write("> - <");
                                for (int b = 0; b < endhvc.size(); b++) {
                                    bw1.write(endhvc.get(b) + ",");
                                }
                                bw1.write("><pt:" + tempCand4.getstart_pt() + " - " + tempCand4.getend_pt() + ">]\n");
                            }
                            if (markifcounted) {
                                bw1.write(" Was Counted");
                                markifcounted = false;
                            }
                            //bw1.write("\n");
                            bw1.newLine();
                            //clear token
                            token = new Token(sysathand.GetNumberOfProcesses());
                        }//end of found/reported pred satisfaction
                    } else//not token owner-token was handed over to another process (withing missing representative Or red candidate representative) or I(procid) don't have the right candidate yet-red
                    {
                        if (token.getCandidateAt(proc_id).getcolor() == "red") {
                            nomorecand = true;
                        }
                    }
                    //keep token at the new owner
                    proc_id = token.getTokenOwner();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                // always close the file
                if (bw1 != null)
                    try {
                        bw1.close();
                    } catch (IOException ioe2) {
                        ioe2.printStackTrace();
                    }
            }
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
                    //updatedhvc.set(pr,Math.max(correspSendHVC.getHvc().get(pr),proc.getHvc().get(pr)));
                    updatedhvc.add(Math.max(correspSendHVC.getHvc().get(pr),proc.getHvc().get(pr)));
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
                    MessageSendStruct correspSendHVC = senderproc.getHVCfromQueue(sender_time);
                }
                proc.updateClock(receiver_time,false,sysathand.GetNumberOfProcesses());
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
            if(!bmisc)
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
}