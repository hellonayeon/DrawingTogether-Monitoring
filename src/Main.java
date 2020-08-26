import DBReader.DBReader;
import LogReader.LogReader;
import MQTTClient.MQTTClient;

public class Main {
   
   /*
        -i <interval> : 지정한 시간 간격으로 DB 업데이트
    */

   public static void main(String[] args) {

      int interval = 3;

      for (int i = 0; i < args.length; i++) {
         if (args[i].charAt(0) == '-') {
            if (args[i].length() < 2) {
               System.out.println("inappropriate " + args[i]);
               return;
            }

            switch (args[i].charAt(1)) {
            case 'i':
               if (args[i].length() < 3) {
                  try {
                     interval = Integer.parseInt(args[i + 1]);

                  } catch (ArrayIndexOutOfBoundsException e) {
                     System.out.println(args[i] + " requires argument");
                     return;
                  } catch (NumberFormatException e) {
                     System.out.println("invalid format " + args[i + 1]);
                     return;
                  }
               } else {
                  String it = args[i].replace("-i", "");
                  try {
                     interval = Integer.parseInt(it);
                  } catch (NumberFormatException e) {
                     System.out.println("invalid format " + it);
                     return;
                  }
               }
               break;


            case '-':
               if (args[i].contains("help"))
                  printHelp();
               return;
            default:
               System.out.println("unknown option :" + args[i]);
               printHelp();
               return;
            }
         }
      }
      System.out.println("Starting...");
      System.out.println("interval : " + interval);



      LogReader logReader = new LogReader(interval);
      Thread t = new Thread(logReader);
      t.start();

      //24시간 주기로 실시간 테이블을 비우고 데이터셋을 가공하여 통계 테이블로 옮김 
      DBReader dbReader = new DBReader();
      Thread t2 = new Thread(dbReader);
      t2.start();
      
      MQTTClient mqttClient = new MQTTClient("192.168.0.36", "1883");
      
   }

   public static void printHelp() {
      System.out.println("Usage: [-i] [-]");
      System.out.println("  -i <interval> : 지정한 시간 간격으로 DB 업데이트");

   }
}