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

    /*method to check overlap - COMPUTE_IF_OVERLAP_IN_TOKEN*/
    boolean computeIfOverap(int tokenatprocid, int syseps)
    {
        //case 1: candidate is not set for some process
        for(int i=0;i<token.size();i++)//loop through token entries
        {
            if (!representativeIsSetAt(i))
            {
                setTokenOwner(i);
                //System.out.println("Process"+i+""+representativeIsSetAt(i)+"\n");
                return false;
            }
            //System.out.println("Process"+i+""+representativeIsSetAt(i)+"\n");
        }

        if(TraceHVCTimestampingGargToken.debugmode==1)
        {
            //JUST PRINTING FOR DEBUGGING
            try
            {
                BufferedWriter candbw1= new BufferedWriter(new FileWriter("Tokens_hvc.txt", true));//true for append
                candbw1.append("Token:\n");
                for(int i=0;i<token.size();i++)//loop through token entries
                {
                    if(i==getTokenOwner())
                    {
                        candbw1.append(i+":<"+(token.get(i)).getstart_hvc()+"> to <"+(token.get(i)).getend_hvc()+">;"+(token.get(i)).getcolor()+"\n");
                    }
                    else
                    {
                        candbw1.append(i+":<"+(token.get(i)).getstart_hvc()+"> to <"+(token.get(i)).getend_hvc()+">;"+(token.get(i)).getcolor()+"; Owner \n");
                    }
                }
                candbw1.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        //check if candidate of current token owner process does not happen before candidates of other processes
        //if no more candidate then set color to red
        Candidate currentcand=getCandidateAt(tokenatprocid);
        currentcand.setcolor("green");
        representativeSetCandidateAt(tokenatprocid,currentcand);
        for(int i=0;i<token.size();i++)
        {
            if(i!=tokenatprocid)
            {
                int chkhb=getCandidateAt(i).happenedBefore(getCandidateAt(tokenatprocid));
                //if(getCandidateAt(i).happenedBefore(getCandidateAt(tokenatprocid))==1)//i's candidate happened before tokenprocid's candidate
                if((chkhb==1)||(getCandidateAt(i).epsBehind(getCandidateAt(tokenatprocid),syseps)))//i's candidate happened before tokenprocid's candidate or i's candidate is more than epsilon behind
                {
                    Candidate otcand=getCandidateAt(i);
                    otcand.setcolor("red");
                    representativeSetCandidateAt(i,otcand);
                }
                else if((chkhb==-1)||(getCandidateAt(tokenatprocid).epsBehind(getCandidateAt(i),syseps)))//tokenprocid's candidate happened before i's candidate  or is more than epsilon behind
                {
                    //if no more candidate then set color to red
                    currentcand.setcolor("red");
                    representativeSetCandidateAt(tokenatprocid,currentcand);
                }
                else
                {
                    //0 is good-means cut is consistent so far
                }
            }
        }
        return true;
    }
}