import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Vector;

//class called Token- <one candidate for each process>
class Token
{
    Vector<Candidate> token;
    int ownerId;
    Token(int numofproc)
    {
        Vector<Integer> starthvc=new Vector<Integer>(numofproc);
        Vector<Integer> endhvc=new Vector<Integer>(numofproc);
        for(int i=0; i<numofproc;i++)
        {
            starthvc.add(-1);
            endhvc.add(-1);
        }
        token= new Vector<Candidate>();
        Candidate newCand= new Candidate(starthvc,endhvc,-1,-1,"red");
        for(int i=0; i<numofproc;i++)
        {
            token.add(newCand);
        }
        ownerId=0;
    }
    Vector<Integer> computelargestend()
    {
        Vector<Integer> largeendinfo=new Vector<Integer>(2);
        int largestptseensofar=0;
        int correspproc=-1;
        for(int ind=0;ind<token.size();ind++)
        {
            if(largestptseensofar<token.get(ind).getend_pt())
            {
                largestptseensofar=token.get(ind).getend_pt();
                correspproc=ind;
            }
        }
        largeendinfo.add(largestptseensofar);
        largeendinfo.add(correspproc);
        return largeendinfo;
    }
    Vector<Integer> computesmalleststart()
    {
        Vector<Integer> smalleststartinfo=new Vector<Integer>(2);
        int smallestptseensofar=Integer.MAX_VALUE;
        int correspproc=-1;
        for(int ind=0;ind<token.size();ind++)
        {
            if(smallestptseensofar>token.get(ind).getstart_pt())
            {
                smallestptseensofar=token.get(ind).getstart_pt();
                correspproc=ind;
            }
        }
        smalleststartinfo.add(smallestptseensofar);
        smalleststartinfo.add(correspproc);
        return smalleststartinfo;
    }
    void representativeSetCandidateAt(int procid, Candidate procCand)
    {
        token.set(procid,procCand);
    }
    boolean representativeIsSetAt(int procid)
    {
        if(token.get(procid).getend_pt()==-1)
        {
            return false;
        }
        else
        {
            //System.out.println("--->"+token.get(procid).getend_l());
            return true;
        }
    }
    Candidate getCandidateAt(int procid)
    {
        return token.get(procid);
    }

    void setTokenOwner(int pid)
    {
        ownerId=pid;
    }
    int getTokenOwner()
    {
        return ownerId;
    }

    int getWindow(int syseps)
    {
        int smallestptincut=computesmalleststart().get(0);
        int window=smallestptincut/syseps;
        //System.out.println("smallestptincut:"+smallestptincut+";syseps:"+syseps+";Window:"+window);
        return window;
    }

    int isIncompleteAt(){
        for(int i=0;i<token.size();i++)//loop through token entries
        {
            if (!representativeIsSetAt(i))
            {
                //System.out.println("Process"+i+""+representativeIsSetAt(i)+"\n");
                return i;
            }
            //System.out.println("Process"+i+""+representativeIsSetAt(i)+"\n");
        }
        return -1;
    }

    int getFirstRedProcess(){
        for(int i=0;i<token.size();i++)//loop through token entries
        {
            if (token.elementAt(i).getcolor() == "red")
            {
                return i;
            }
            //System.out.println("Process"+i+""+representativeIsSetAt(i)+"\n");
        }
        return -1;
    }

    /*method to check overlap - COMPUTE_IF_OVERLAP_IN_TOKEN*/
    boolean computeIfValidCandidate(int tokenatprocid, int syseps)
    {
        //case 1: candidate is not set for some process
        if(isIncompleteAt()!=-1){
            return false;
        }
        if(TraceHVCTimestampingGargToken.debugmode==1)
        {
            String tokens_file = TraceHVCTimestampingGargToken.inpfilename.substring(0, TraceHVCTimestampingGargToken.inpfilename.lastIndexOf('.'))+"\\Tokens_hvc.txt";
            //JUST PRINTING FOR DEBUGGING
            print(tokens_file);
        }
        //check if candidate of current token owner process does not happen before candidates of other processes
        //if no more candidate then set color to red
        Candidate currentcand=getCandidateAt(tokenatprocid);
        currentcand.setcolor("green");
        representativeSetCandidateAt(tokenatprocid,currentcand);
        int nextOwner = -1;
        boolean valid = true;
        for(int i=0;i<token.size();i++)
        {
            if(i!=tokenatprocid)
            {
                int chkhb=getCandidateAt(tokenatprocid).happensBefore(getCandidateAt(i), syseps);
                //if(getCandidateAt(i).happenedBefore(getCandidateAt(tokenatprocid))==1)//i's candidate happened before tokenprocid's candidate
                if(chkhb==-1)//i's candidate happened before tokenprocid's candidate or i's candidate is more than epsilon behind
                {
                    Candidate otcand=getCandidateAt(i);
                    otcand.setcolor("red");
                    if(nextOwner== -1){
                        //System.out.println("Candidate at P"+i+" is first one identified to be behind");
                        //otcand.print_start_hvc();
                        //otcand.print_end_hvc();
                        nextOwner = i;
                    }
                    representativeSetCandidateAt(i,otcand);
                    valid = false;
                }
                else if(chkhb==1)//tokenprocid's candidate happened before i's candidate  or is more than epsilon behind
                {
                    //if no more candidate then set color to red
                    currentcand.setcolor("red");
                    representativeSetCandidateAt(tokenatprocid,currentcand);
                    return false;
                }
                else
                {
                    //0 is good-means cut is consistent so far
                }
            }
        }
        if (nextOwner!=-1){ //should be executed only if found is false and non-token-owner process is red
            ownerId = nextOwner; //set the token owner appropriately
        }
        return valid;
    }
    void print(String filename){
        //JUST PRINTING FOR DEBUGGING
        try
        {
            BufferedWriter candbw1= new BufferedWriter(new FileWriter(filename, true));//true for append
            candbw1.append("Token at Process: "+ownerId+"\n");
            for(int i=0;i<token.size();i++)//loop through token entries
            {
                Candidate tempCand4 = getCandidateAt(i);
                Vector<Integer> sthvc = tempCand4.getstart_hvc();
                Vector<Integer> endhvc = tempCand4.getend_hvc();
                candbw1.write("[P" + i + ":<");
                for (int b = 0; b < sthvc.size(); b++) {
                    candbw1.write(sthvc.get(b) + ",");
                }
                candbw1.write("> - <");
                for (int b = 0; b < endhvc.size(); b++) {
                    candbw1.write(endhvc.get(b) + ",");
                }
                candbw1.write("><pt:" + tempCand4.getstart_pt() + " - " + tempCand4.getend_pt() + ">];"+(token.get(i)).getcolor()+"\n");
            }
            candbw1.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    void markAs(String filename, String label){
        //JUST PRINTING FOR DEBUGGING
        try
        {
            BufferedWriter candbw1= new BufferedWriter(new FileWriter(filename, true));//true for append
            candbw1.append(label+".\n");
            candbw1.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    void printToConsole(){
        for(int i=0;i<token.size();i++)//loop through token entries
        {
            Candidate tempCand4 = getCandidateAt(i);
            Vector<Integer> sthvc = tempCand4.getstart_hvc();
            Vector<Integer> endhvc = tempCand4.getend_hvc();
            System.out.print("[P" + i + ":<");
            for (int b = 0; b < sthvc.size(); b++) {
                System.out.print(sthvc.get(b) + ",");
            }
            System.out.print("> - <");
            for (int b = 0; b < endhvc.size(); b++) {
                System.out.print(endhvc.get(b) + ",");
            }
            System.out.print("><pt:" + tempCand4.getstart_pt() + " - " + tempCand4.getend_pt() + ">];"+(token.get(i)).getcolor()+"\n");
        }
    }
    void clearCandidateAt(int proc, int numofproc){
        Vector<Integer> starthvc=new Vector<Integer>(numofproc);
        Vector<Integer> endhvc=new Vector<Integer>(numofproc);
        for(int i=0; i<numofproc;i++)
        {
            starthvc.add(-1);
            endhvc.add(-1);
        }
        Candidate newCand= new Candidate(starthvc,endhvc,-1,-1,"red");
        representativeSetCandidateAt(proc,newCand);
    }

}