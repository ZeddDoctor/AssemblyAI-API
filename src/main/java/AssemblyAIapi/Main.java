package AssemblyAIapi;

import java.util.Scanner;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import org.apache.commons.validator.routines.UrlValidator;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        Transcript transcript = new Transcript();
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter your AssemblyAI API key: ");
        String api_key = sc.nextLine();
        if(api_key.length()!=32){
            System.out.println("Invalid API key(not 32 characters)");
            System.exit(1);
        }
        Constants.setApiKey(api_key);

        System.out.print("Enter audio url to convert to text: ");
        String audio_url = sc.nextLine();
        if(!UrlValidator.getInstance().isValid(audio_url)){
            System.out.println("Invalid url");
            System.exit(1);
        }
        transcript.setAudio_url(audio_url);
        //https://github.com/johnmarty3/JavaAPITutorial/raw/main/Thirsty.mp4

        Gson gson = new Gson();
        String jsonRequest = gson.toJson(transcript);
        //System.out.println(jsonRequest);//For dev purposes

        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(new URI("https://api.assemblyai.com/v2/transcript"))
                .header("Authorization", Constants.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> postResponse = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

        if(postResponse.statusCode() != 200){
            System.out.println("Bad response:" + postResponse.statusCode());
            System.exit(1);
        }
        //System.out.println(postResponse.body());//For dev purposes

        transcript = gson.fromJson(postResponse.body(), Transcript.class);
        if("error".equals(transcript.getStatus())){
            System.out.println("Something went wrong on the server side\n" + postResponse.body());
            System.exit(1);
        }
        System.out.println("Your request id:" + transcript.getId());

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(new URI("https://api.assemblyai.com/v2/transcript/" + transcript.getId()))
                .header("Authorization", Constants.getApiKey())
                .build();


        while(true){
            HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
            transcript = gson.fromJson(getResponse.body(), Transcript.class);

            System.out.println(transcript.getStatus());
            if ("completed".equals(transcript.getStatus()) || "error".equals(transcript.getStatus()))
                break;

            Thread.sleep(1500);
        }
        if("error".equals(transcript.getStatus())){
            System.out.println("Something went wrong on the server side\n" + gson.toJson(transcript));
            System.exit(1);
        }
        System.out.println("Transcription completed!");
        System.out.println("Audio to text:\n\n" + transcript.getText());

    }
}
