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
    int happenedBefore(Vector<Integer> mystarthvc, Vector<Integer> otherstarthvc)
    {
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
    int getLargerStartPT(Candidate othercand){
        return Math.max(othercand.getstart_pt(),getstart_pt());
    }
    int getSmallerEndPT(Candidate othercand){
        return Math.min(othercand.getend_pt(),getend_pt());
    }
    int happensBefore(Candidate othercand, int epsi){
        Vector<Integer> otherstarthvc=othercand.getstart_hvc();
        //Vector<Integer> otherendhvc=othercand.getend_hvc();
        Vector<Integer> mystarthvc=start_hvc;
        int startptofothercand=othercand.getstart_pt();
        int myendpt = getend_pt();
        int hb = 0;
        int causality = happenedBefore(mystarthvc,otherstarthvc);
        //compute distance in terms of physical time
        int largerStartPT = getLargerStartPT(othercand);
        int smallerEndPT = getSmallerEndPT(othercand);
        int ptDistance=0;
        if ((largerStartPT == othercand.getstart_pt() && smallerEndPT== getend_pt()) ||(largerStartPT == getstart_pt() && smallerEndPT== othercand.getend_pt())){
            ptDistance = largerStartPT - smallerEndPT;
        } //else there is an overlap in physical time of the intervals - one candidate interval is completely overlapped by the other
        if((causality ==1) ||((ptDistance > epsi) && (smallerEndPT==getend_pt()))){ //negative ptDistance indicates an overlap
            hb = 1;
        } else if ((causality == -1) ||((ptDistance > epsi) && (smallerEndPT==othercand.getend_pt()))){
            hb = -1;
        }
        return hb;
    }
}
