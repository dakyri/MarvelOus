package com.mayaswell.marvelous;

import junit.framework.TestCase;
import com.mayaswell.marvelous.MarvelAPI.CharacterResponse;
import com.mayaswell.marvelous.MarvelAPI.ImageSize;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * unit tests for main api
 * Created by dak on 10/23/2016.
 */
public class MarvelAPITest extends TestCase {
	private MarvelAPI marvelAPI;
	String datapool = "{"+
			"\"code\":200,"+
			"\"status\":\"ok\","+
			"\"copyright\":\"pk\","+
			"\"attributionText\":\"qk\","+
			"\"attributionHTML\":\"rk\","+
			"\"data\":{"+
			"\"offset\":0,"+
			"\"limit\":0,"+
			"\"total\":1,"+
			"\"count\":1,"+
			"\"results\":"+"["+
			"{\"id\":1009268,\"name\":\"Deadpool\",\"description\":\"\",\"modified\":\"2013-10-18T17:33:26-0400\",\"thumbnail\":{\"path\":\"http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99\",\"extension\":\"jpg\"},\"resourceURI\":\"http://gateway.marvel.com/v1/public/characters/1009268\""+"}"+
			"]"+
			"}"+
			"}";

	@Before
	public void setUp() throws Exception {
		marvelAPI = new MarvelAPI(
				"http://gateway.marvel.com/v1/public/",
				"d69ae1426b19ec1650e79780e2fac09c",
				"24e1aa65ba9828af4cd7415969bcff12d67cc696");
	}


	@Test
	public void testParseCharacterResponse() throws Exception {
		CharacterResponse cr = MarvelAPI.parseCharacterResponse(datapool);
		assertEquals(cr.code, 200);
		assertEquals(cr.status, "ok");
		assertEquals(cr.copyright, "pk");
		assertEquals(cr.attributionText, "qk");
		assertEquals(cr.attributionHTML, "rk");
		assertEquals(cr.data.results.size(), 1);
		MarvelAPI.Character c = cr.data.results.get(0);
		assertNotNull(c);
		assertEquals(c.id, 1009268);
		assertEquals(c.name, "Deadpool");
		assertEquals(c.description, "");
		assertEquals(c.resourceURI, "http://gateway.marvel.com/v1/public/characters/1009268");
	}

	@Test
	public void testImagePath() throws Exception {
		CharacterResponse cr = MarvelAPI.parseCharacterResponse(datapool);
		MarvelAPI.Character c = cr.data.results.get(0);
		assertNotNull(c);
		MarvelAPI.Image i = c.thumbnail;
		assertEquals(i.getURL(ImageSize.PORTRAIT_S), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/portrait_small.jpg");
		assertEquals(i.getURL(ImageSize.PORTRAIT_M), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/portrait_medium.jpg");
		assertEquals(i.getURL(ImageSize.PORTRAIT_L), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/portrait_large.jpg");
		assertEquals(i.getURL(ImageSize.PORTRAIT_XL), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/portrait_xlarge.jpg");
		assertEquals(i.getURL(ImageSize.STANDARD_S), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/standard_small.jpg");
		assertEquals(i.getURL(ImageSize.STANDARD_M), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/standard_medium.jpg");
		assertEquals(i.getURL(ImageSize.STANDARD_L), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/standard_large.jpg");
		assertEquals(i.getURL(ImageSize.STANDARD_XL), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/standard_xlarge.jpg");
		assertEquals(i.getURL(ImageSize.LANDSCAPE_S), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/landscape_small.jpg");
		assertEquals(i.getURL(ImageSize.LANDSCAPE_M), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/landscape_medium.jpg");
		assertEquals(i.getURL(ImageSize.LANDSCAPE_L), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/landscape_large.jpg");
		assertEquals(i.getURL(ImageSize.LANDSCAPE_XL), "http://i.annihil.us/u/prod/marvel/i/mg/9/90/5261a86cacb99/landscape_xlarge.jpg");
	}

	@Test
	public void testMd5() throws Exception {
		assertEquals("", MarvelAPI.md5(null));
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", MarvelAPI.md5(""));
		assertEquals("104b424f0f95fbb95e943b8035566fb9", MarvelAPI.md5("d69ae1426b19ec1650e79780e2fac09c"));
		assertEquals("d7f92fc5a9e66556adbd10cdd5da15e4", MarvelAPI.md5("24e1aa65ba9828af4cd7415969bcff12d67cc696"));
	}
}