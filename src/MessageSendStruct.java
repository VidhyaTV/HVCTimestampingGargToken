import java.util.Vector;

class MessageSendStruct
{
    int msgid;
    int pt;
    Vector<Integer> hvc;
    MessageSendStruct(int mid, int ptvalue, Vector<Integer> msghvc)
    {
        msgid=mid;
        pt=ptvalue;
        hvc=msghvc;
    }
    int getMsgid(){return msgid;}
    int getPt(){return pt;}
    Vector<Integer> getHvc(){return hvc;}
}