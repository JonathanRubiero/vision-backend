package com.dev.gr.strategie.rest.test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;
import static com.dev.gr.strategie.rest.service.utils.Utils.*;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dev.gr.strategie.rest.service.Agent;
import com.dev.gr.strategie.rest.service.data.Playlist;
import com.jayway.restassured.http.ContentType;

public class TestAgent {

	private static final String BASE_URL = baseUrl();
	
	private static Agent agent;
	
	@Before
	public void before() {
	}
	
	@BeforeClass
	public static void start() {
		try {
			FileUtils.copyToDirectory(FileUtils.listFiles(testData(), null, false), data());
		} catch (IOException e) {
			e.printStackTrace();
		}			
		agent = new Agent();
	}
	
	@After
	public void after() {
	}

	@AfterClass
	public static void stop() {
		agent.stopAgent();
		FileUtils.listFiles(data(), null, false).
			stream().
			filter(f -> f.isFile()).
			forEach(f -> FileUtils.deleteQuietly(f));
		
	}

	@Test
	public void testListFile() {
		given().
			log().ifValidationFails().
		when().
			get(buildURL("/files")).
		then().
			statusCode(200).
			body("status", equalTo("SUCCESS")).
			body("data", hasItem("testFile.txt"));
	}
	
	@Test
	public void testUploadFile() {
		given().
			contentType("multipart/form-data").
			multiPart("file", dataPath().resolve("testFile.txt").toFile()).
		when().
			post(buildURL("/files")).
		then().
			statusCode(200);
	}
		
	@Test
	public void  testDownloadFile() throws IOException {
		String fileName = "testDownloadFile.zip";
		File sourceFile = dataPath().resolve(fileName).toFile();
		File downloadedFile = dataPath().resolve(suffixFileName(fileName, "_downloaded")).toFile();
		try(InputStream is =
			given().
				log().ifValidationFails().
			when().
				get(buildURL("/files/" + fileName)).
			then().
				statusCode(200).
			extract().
				response().asInputStream()) {			
			FileUtils.copyInputStreamToFile(is, downloadedFile);		
			assertTrue(FileUtils.contentEquals(sourceFile, downloadedFile));
			fileName = "notExistingFile.txt";
			given().
				log().ifValidationFails().
			when().
				get(buildURL("/files/" + fileName)).
			then().
				statusCode(404).
				body("status", equalTo("ERROR")).
				body("data", containsString("NoSuchFileException"));
		} 
	}
	
	@Test
	public void  testDeleteFile() {
		String fileName = "testDeleteFile.txt";
		given().
			log().ifValidationFails().
		when().
			delete(buildURL("/files/" + fileName)).
		then().
			statusCode(200).
			body("status", equalTo("SUCCESS"));
		
		fileName = "notExistingFile.txt";
		given().
			log().ifValidationFails().
		when().
			delete(buildURL("/files/" + fileName)).
		then().
			log().ifValidationFails().
			statusCode(404).
			body("status", equalTo("ERROR")).
			body("data", containsString("NoSuchFileException"));
	}
	
	@Test
	public void testSchedulePlaylist() throws InterruptedException {
		Playlist playlist = new Playlist("playlist1", Arrays.asList("testFile1.txt", "testFile2.txt"), "0/2 * * * * ?", "1/4 * * * * ?");
		given().
			log().ifValidationFails().
			contentType(ContentType.JSON).
			body(playlist).
		when().
			post(buildURL("/playlist/schedule")).
		then().
			log().ifValidationFails().
			statusCode(200).
			body("status", equalTo("SUCCESS"));
			
		Thread.sleep(5000);	
		testRemovePlaylist();
	}
	
	@Test
	public void testRemovePlaylist() {
		String playlistName = "playlist1";
		given().
			log().ifValidationFails().
		when().
			delete(buildURL("/playlist/" + playlistName)).
		then().
			statusCode(200).
			body("status", equalTo("SUCCESS"));
		
	}
	
	public static final String buildURL(String uri) {
		return new StringBuilder(BASE_URL)
				.append(uri)
				.toString();
	}
}