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

      MQTTClient mqttClient = new MQTTClient(interval);
      Thread t = new Thread(mqttClient);
      t.start();

      LogReader logReader = new LogReader(interval, 100);
      Thread t2 = new Thread(logReader);
      t2.start();
      
      
      
   }

   public static void printHelp() {
      System.out.println("Usage: [-i] [-]");
      System.out.println("  -i <interval> : 지정한 시간 간격으로 DB 업데이트");

   }
}