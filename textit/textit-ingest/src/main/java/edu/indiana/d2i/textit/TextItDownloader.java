package edu.indiana.d2i.textit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.InputStream;
import java.util.Properties;

public class TextItDownloader {
	static TextItWebHook hook = null;
    private static Logger logger = Logger.getLogger(TextItDownloader.class);
	
	public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("./conf/log4j.properties");

		if (args.length != 2 && args.length != 3) {
			logger.error("Usage: [config_file] [weekly|daily] [port]");
			System.exit(-1);
		}

        if(!args[1].equals("daily") && !args[1].equals("weekly")){
            logger.error("Error: Second argument to the TextItDownloader should be either 'daily' or 'weekly'");
            System.exit(-1);
        }

        logger.info("Starting TextItDownloader...");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Clean up resources.");
                if (hook != null)
                    try {
                        hook.stop();
                    } catch (Exception e) {
                        System.err.println(e.getStackTrace());
                    }
            }
        });

        Properties properties = new Properties();
        InputStream stream = TextItClient.class.getClassLoader()
                .getResourceAsStream(args[0]);
        if (stream == null) {
            throw new RuntimeException("Error : " + args[0] + " is not found!");
        }
        if(args[1].equals("daily")) {
            properties.put("download_no_of_days", "1");
        } else {
            properties.put("download_no_of_days", "7");
        }
        properties.load(stream);
        stream.close();
		
		if (args.length == 2) {
            logger.info("Just download the runs.");
			TextItClient client = TextItClient.createClient(properties);
			client.downloadRuns();
			client.close();
		} else {
			// TODO: check if there is any run before, try to resume first
            logger.info("Download the runs first and then runs as a callback service.");
			
			// start downloading from scratch
			TextItClient client = TextItClient.createClient(properties);
			client.downloadRuns();
			client.close();
			
			// run the web hook
			int port = Integer.valueOf(args[2]);
			hook = TextItWebHook.getSingleton(properties, port);
			hook.start();
		}
        logger.info("Finished TextItDownloader...");
    }
}
