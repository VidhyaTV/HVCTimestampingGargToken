import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Vector;

class Process
{
    int id;
    Vector<Integer> hvc;
    int pt;

    //variable to remember previous interval's end (current interval's start) when a message is received/sent
    Vector<Integer> prev_hvc;
    int prev_pt;

    //queue to remember interval-candidates
    Deque<Candidate> candQueue;

    //queue to remember l,c values corresponding to message sends
    Deque<MessageSendStruct> hvclog;
    int msg_counter;

    //boolean reported_interval_already;//variable to track first ever interval reported at a process

    int lastsendorrecorlocevntpt;//variable to check if multiple events -msg send/rcv or local event happened at the same instant --used to update prev(Old) pt,hvc only when the first event occurs at a specific physical time

    //variable to help ignore true intervals at a specific frequency
    int acceptInterval;	
    int lastAcceptedStartPt;
    int lastIgnoredStartPt;
    int ignoredMsgCnt;	
    Process(int unique_id, Vector<Integer> proc_hvc, int phytime)
    {
        id=unique_id;
        hvc=proc_hvc;
        pt=phytime;

        msg_counter=0;

        prev_hvc=proc_hvc;//assuming that proc_hvc is also <0,0,...0,0>

        prev_pt=0;
        hvclog = new ArrayDeque<MessageSendStruct>();
        candQueue= new ArrayDeque<Candidate>();

        //reported_interval_already=false;

        lastsendorrecorlocevntpt=-1;
		acceptInterval=0;		
        lastAcceptedStartPt=0;
        lastIgnoredStartPt=0;
		ignoredMsgCnt=0;
    }

    void setId(int passed_id){id=passed_id;}
    void setHvc(Vector<Integer> passed_hvc){hvc=passed_hvc;}
    void setPt(int passed_pt){pt=passed_pt;}
    void setlastsendorrecorlocevntpt(int sendreclocventpt){lastsendorrecorlocevntpt=sendreclocventpt;}
    void setOldHvc(Vector<Integer> passed_hvc){prev_hvc=passed_hvc;}
    void setOldPt(int passed_pt){prev_pt=passed_pt;}
    void setAcceptInterval(int value){acceptInterval=value;}
	void setLastAcceptedStartPt(int startPt){lastAcceptedStartPt=startPt;}
    void setLastIgnoredStartPt(int startPt){lastIgnoredStartPt=startPt;}
	void setIgnoredMsgCnt(int cnt){ignoredMsgCnt=cnt;}
    int getIgnoredMsgCnt(){return ignoredMsgCnt;}
	int getLastIgnoredStartPt(){return lastIgnoredStartPt;}
    int getLastAcceptedStartPt(){return lastAcceptedStartPt;}
    int getAcceptInterval(){return acceptInterval;}
    int getId(){return id;}
    Vector<Integer> getHvc(){return hvc;}
    int getPt(){return pt;}

    Vector<Integer> getOldHvc(){return prev_hvc;}
    int getOldPt(){return prev_pt;}
    int getlastsendorrecorlocevntpt(){return lastsendorrecorlocevntpt;}

    int getCandQueueSize(){
        return  candQueue.size();
    }
    boolean isCandidateQEmpty(){
        return candQueue.isEmpty();
    }
    Candidate getFirstCandidate(){
        return  candQueue.pollFirst();
    }

    //boolean getReportedIntervalAlready(){return reported_interval_already;}
    //void setReportedIntervalAlready(){reported_interval_already=true;}

    void updateClock(int physicalTime, boolean sendmsg, int numofproc)
    {
        Vector<Integer> currenthvc=new Vector<Integer>(numofproc);
        for(int huj=0;huj<numofproc;huj++)
        {
            //currenthvc.set(huj,getHvc().get(huj));
            currenthvc.add(getHvc().get(huj));
        }

        if(lastsendorrecorlocevntpt!=physicalTime)
        {
            setOldPt(getPt());
            setOldHvc(currenthvc);
        }
        Vector<Integer> updatedhvc=new Vector<Integer>(numofproc);
        for(int pr=0;pr<numofproc;pr++)
        {
            if(pr==id)
            {
                //updatedhvc.set(id,(updatedhvc.get(id))+1);
                updatedhvc.add((getHvc().get(id))+1);
            }
            else
            {
                //updatedhvc.set(pr,getHvc().get(pr));
                updatedhvc.add(getHvc().get(pr));
            }
        }
        setHvc(updatedhvc);
        setPt(physicalTime);
		/*
		System.out.println("Sengmsg:"+sendmsg+"; Updated clock at p"+id+" to :"+currenthvc);
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		*/
        if(sendmsg)
        {
            //push message id, l, c into queue
            MessageSendStruct newmsg= new MessageSendStruct(msg_counter++,getPt(),updatedhvc);
            hvclog.add(newmsg);
        }

        setlastsendorrecorlocevntpt(physicalTime);
    }
    MessageSendStruct getHVCfromQueue(int passed_phytime)
    {
        while(hvclog.peek().getPt()!=passed_phytime)
        {
            //System.out.println(passed_phytime+","+hvclog.peek().getPt());
            System.out.println("FIFO VIOLATED...popping..");
            hvclog.pop();
        }
        MessageSendStruct msgpthvc=hvclog.peek();
        if(msgpthvc!=null)
        {
            if(passed_phytime == msgpthvc.getPt())
            {
                //System.out.println("FOUND MATCHING SEND");
                return hvclog.pop();
            }
            else
            {
                System.out.println("CODE THAT SHOULD NOT EXECUTE");
                System.exit(0);
            }
            return hvclog.peek();
        }
        else
        {
            System.out.println("SEND QUEUE EMPTY");
            System.exit(0);
            return msgpthvc;
        }
    }

    //at a process	//-- it sees a new candidate
    void newCandidateOccurance(Vector<Integer> intervalstarthvc, Vector<Integer> intervalendhvc, int intervstart_pt, int intervend_pt)
    {
        //System.out.println("At P"+getId()+" Old hvc:"+intervalstarthvc);
        //System.out.println("At P"+getId()+" New hvc:"+intervalendhvc);
        Candidate newCand= new Candidate(intervalstarthvc, intervalendhvc, intervstart_pt, intervend_pt,"red");
        candQueue.add(newCand);
        if(TraceHVCTimestampingGargToken.debugmode==2)
        {
            //JUST FOR DEBUGGING
            try
            {
                //System.out.println("Pushing Candidate");
                BufferedWriter candbw2= new BufferedWriter(new FileWriter("Candidate_hvc"+id+".txt", true));//true for append
                candbw2.append("<"+intervalstarthvc+"> to <"+intervalendhvc+">; <pt:"+intervstart_pt+" to "+intervend_pt+">\n");
                candbw2.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}