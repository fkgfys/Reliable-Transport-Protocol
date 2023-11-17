import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
    public static int sendBase;
    public static int expectedSeqNum;
    public static int nextSeqNum;
    public static Packet[] sndpkt;
    public static Packet[] sndack;
    public static double[] times;

    public static int numPkt;
    public static int retrans;
    public static int numDel;
    public static int numACK;
    public static int numCorup;

    public static int actualBase;
    public static int actualSeqNum;
    public static int actualACKNum;


    public double lossRate(double r) {
        if (r < 0) {
            return 0;
        } else {
            return r;
        }
    }

    public double avgRTT(double[] tms) {
        double sum = 0;
        double count = 0;
        for (int i = 0; i < tms.length; i += 2) {
            if (tms[i + 1] - tms[i] <= 20.0 && tms[i] != 0 && tms[i + 1] != 0) {
                sum += tms[i + 1] - tms[i];
                count++;
            }
        }
        return sum/count;
    }
    
    public double avgCom(double[] tms) {
        double sum = 0;
        double count = 0;
        for (int i = 0; i < tms.length; i += 2) {
            if (tms[i] != 0 && tms[i + 1] != 0) {
                sum += tms[i + 1] - tms[i];
                count++;
            }
        }
        return sum/count;
    }

    public static int getChecksum(Packet pkt) {

        int sum = 0;
        for (int i = 0; i < pkt.getPayload().length(); i++) {
            sum += pkt.getPayload().charAt(i);
        }
        return sum + pkt.getSeqnum() + pkt.getAcknum();
        
    }

    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize*2; // set appropriately
	RxmtInterval = delay;
    }

    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        if (nextSeqNum < sendBase + WindowSize && nextSeqNum < LimitSeqNo) {
            int sum = 0;
            for (int i = 0; i < 20; i++) {
                sum += message.getData().charAt(i);
            }
            int cs = sum + nextSeqNum + actualSeqNum;
            Packet pkt = new Packet(nextSeqNum, actualSeqNum, cs, message.getData()); //use acknum field to store the actual seqnum instead of the wrapped-around one
            sndpkt[actualSeqNum] = pkt;
            numPkt++;
            toLayer3(A, pkt);
            times[actualSeqNum * 2] = getTime();
            System.out.println("A: Sended packet " + nextSeqNum + "!!!");
            if (sendBase == nextSeqNum) {
                startTimer(A, RxmtInterval);
            }
            nextSeqNum++;
            actualSeqNum++;
        } else if (nextSeqNum == LimitSeqNo && sendBase == nextSeqNum) {
            System.out.println("A: SeqNo reached the limit. Wraparound!!!");
            sendBase = 0;
            nextSeqNum = 0;
            int sum = 0;
            for (int i = 0; i < 20; i++) {
                sum += message.getData().charAt(i);
            }
            int cs = sum + nextSeqNum + actualSeqNum;
            Packet pkt = new Packet(nextSeqNum, actualSeqNum, cs, message.getData()); //use acknum field to store the actual seqnum instead of the wrapped-around one
            sndpkt[actualSeqNum] = pkt;
            numPkt++;
            toLayer3(A, pkt);
            times[actualSeqNum * 2] = getTime();
            System.out.println("A: Sended packet " + nextSeqNum + "!!!");
            startTimer(A, RxmtInterval);
            nextSeqNum++;
            actualSeqNum++;
        } else if (nextSeqNum >= sendBase + WindowSize) {
            System.out.println("A: Window currently full!!!");
        }
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        if (getChecksum(packet) == packet.getChecksum() && packet.getSeqnum() >= actualBase) {
            sendBase = packet.getAcknum() + 1;
            actualBase = packet.getSeqnum() + 1;
            times[packet.getSeqnum() * 2 + 1] = getTime();
            System.out.println("A: Received an ACK of packet " + packet.getAcknum() + "!!!");
            System.out.println("A: Moved base to " + sendBase + "!!!");
            //System.out.println("A: ActualBase is now " + actualBase + "!!!");
            if (sendBase == nextSeqNum) {
                stopTimer(A);
            } else {
                startTimer(A, RxmtInterval);
            }
        } else if (getChecksum(packet) != packet.getChecksum()) {
            numCorup++;
            System.out.println("Corrupted ACK !!!");
        } /*else if (packet.getSeqnum() < actualBase) {
            retrans++;
            for (int i = actualBase; i < actualSeqNum; i++) {
                toLayer3(A, sndpkt[i]);
                startTimer(A, 30.0);
                System.out.println("A: Resended packet " + sndpkt[i].getSeqnum() + "!!!");
            }
        }*/
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {   
        retrans++;
        for (int i = actualBase; i < actualSeqNum; i++) {
            
            toLayer3(A, sndpkt[i]);
            startTimer(A, RxmtInterval);
            System.out.println("A: Resended packet " + sndpkt[i].getSeqnum() + "!!!");
        }
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        sendBase = FirstSeqNo;
        nextSeqNum = FirstSeqNo;
        sndpkt = new Packet[1000];
        actualSeqNum = 0;
        actualBase = 0;
        RxmtInterval = 30.0;
        times = new double[2000];
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        if (packet.getChecksum() == getChecksum(packet) && packet.getAcknum() == actualACKNum) { // the second condition is because we use the acknum field of the packet from A to store its real seqnum
            System.out.println("B: Correct packet " + expectedSeqNum + ", deliver it to upper layer!!!");
            toLayer5(packet.getPayload());
            numDel++;
            Packet pkt = new Packet(actualACKNum, expectedSeqNum, expectedSeqNum + actualACKNum); //use the seqNum field to store the actual ACK number instead of the wrapped-around one
            sndack[packet.getAcknum()] = pkt;
            toLayer3(B, pkt);
            System.out.println("B: Sended ACK for packet " + expectedSeqNum + "!!!");
            //System.out.println("B: The actual Seqnum of this packet is " + actualACKNum + "!!!");
            numACK++;
            expectedSeqNum++;
            actualACKNum++;
            if (expectedSeqNum == LimitSeqNo) expectedSeqNum = 0;
            System.out.println("B: Now expecting packet " + expectedSeqNum + "!!!");
        } else if (packet.getChecksum() != getChecksum(packet)) {
            numCorup++; 
        } else if (packet.getAcknum() < actualACKNum) {
            System.out.println("B: Duplicate packet " + packet.getSeqnum() + "!!!");
            toLayer3(B, sndack[packet.getAcknum()]);
            numACK++;
            System.out.println("B: Sended ACK for packet " + packet.getSeqnum() + "!!!");
        } else {
            
            Packet pkt = new Packet(actualACKNum - 1, expectedSeqNum - 1, actualACKNum + expectedSeqNum - 2);
            toLayer3(B, pkt);
            numACK++;
            System.out.println("B: Wrong packet. Please resend packet " + expectedSeqNum + "!!!");
        } 
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        expectedSeqNum = 0;
        actualACKNum = 0;
        sndack = new Packet[1000];
    }

    // Use to print final statistics
    protected void Simulation_done()
    {
    	// TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIABLE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
    	System.out.println("\n\n===============STATISTICS=======================");
    	System.out.println("Number of original packets transmitted by A: " + numPkt);
    	System.out.println("Number of retransmissions by A: " + retrans);
    	System.out.println("Number of data packets delivered to layer 5 at B: " + numDel);
    	System.out.println("Number of ACK packets sent by B: " + numACK);
    	System.out.println("Number of corrupted packets: " + numCorup);
    	System.out.println("Ratio of lost packets: " + lossRate((double) (retrans - numCorup) / (double) (numPkt + retrans)));
    	System.out.println("Ratio of corrupted packets: " + ((double) numCorup / (double) (numPkt + retrans)));
    	System.out.println("Average RTT: " + avgRTT(times));
    	System.out.println("Average communication time: " + avgCom(times));
    	System.out.println("==================================================");

    	// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
    	System.out.println("\nEXTRA:");
    	// EXAMPLE GIVEN BELOW
    	//System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>"); 
    }	

}
