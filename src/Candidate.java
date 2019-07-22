import java.util.Vector;

//class called Candidate- <owner_processid,interval_start_time_vector,interval_end_time_vector>
class Candidate
{
    Vector<Integer> start_hvc;
    Vector<Integer> end_hvc;
    int start_pt;
    int end_pt;
    String color;
    Candidate(Vector<Integer> startt, Vector<Integer> endt, int intervalstartpt, int intervalendpt, String col)
    {
        start_hvc=startt;
        end_hvc=endt;
        start_pt=intervalstartpt;
        end_pt=intervalendpt;
        color=col;
    }
    void setstart_hvc(Vector<Integer> starthvc)
    {
        start_hvc=starthvc;
    }
    Vector<Integer> getstart_hvc()
    {
        return start_hvc;
    }
    void print_start_hvc()
    {
        for(int b=0;b<start_hvc.size();b++)
        {
            System.out.print(start_hvc.get(b)+" ");
        }
        System.out.println();
    }
    void setend_hvc(Vector<Integer> endhvc)
    {
        end_hvc=endhvc;
    }
    Vector<Integer> getend_hvc()
    {
        return end_hvc;
    }
    void print_end_hvc()
    {
        for(int b=0;b<end_hvc.size();b++)
        {
            System.out.print(end_hvc.get(b)+" ");
        }
        System.out.println();
    }
    int getstart_pt()
    {
        return start_pt;
    }
    int getend_pt()
    {
        return end_pt;
    }
    void setstart_pt(int newstart_pt)
    {
        start_pt=newstart_pt;
    }
    void setend_pt(int newend_pt)
    {
        end_pt=newend_pt;
    }
    void setcolor(String col)
    {
        color=col;
    }
    String getcolor()
    {
        return color;
    }
    int happenedBefore(Candidate othercand)
    {
        Vector<Integer> otherstarthvc=othercand.getstart_hvc();
        //Vector<Integer> otherendhvc=othercand.getend_hvc();
        Vector<Integer> mystarthvc=start_hvc;
        //Vector<Integer> myendhvc=end_hvc;
        boolean ihappenedbeforeother=false;
        boolean otherhappenedbeforeme=false;
        for(int i=0;i<start_hvc.size();i++)
        {
            if(otherstarthvc.get(i)<mystarthvc.get(i))
            {
                otherhappenedbeforeme=true;
            }
            else if(otherstarthvc.get(i)>mystarthvc.get(i))
            {
                ihappenedbeforeother=true;
            }
            else
            {}
        }

        //if happened before
        if(ihappenedbeforeother && !otherhappenedbeforeme)
        {
            return 1;
        }
        //else if othercand happened before means
        else if (otherhappenedbeforeme && !ihappenedbeforeother)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }
    boolean epsBehind(Candidate othercand, int epsi)
    {
        int startptofothercand=othercand.getstart_pt();
        if(getend_pt()<=startptofothercand)
        {
            if(startptofothercand-getend_pt()<=epsi)//my(current candidate's) endpt is behind other candidates start_pt but within epsilon
            {
                return false;
            }
            else //my(current candidate's) endpt is far behind other candidates start_pt
            {
                return true;
            }
        }
        else//my(current candidate's) endpt is ahead other candidates start_pt
        {
            return false;
        }
    }
}
