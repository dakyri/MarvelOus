package com.mayaswell.marvelous;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import org.w3c.dom.CharacterData;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 *   parsing is via GSON, http handling via okhttp, asynchronous processing and callbacks via rx.
 */
public class MarvelAPI {

	public enum ImageSize {
		PORTRAIT_S("portrait_small"),
		PORTRAIT_M("portrait_medium"),
		PORTRAIT_L("portrait_large"),
		PORTRAIT_XL("portrait_xlarge"),
		STANDARD_S("standard_small"),
		STANDARD_M("standard_medium"),
		STANDARD_L("standard_large"),
		STANDARD_XL("standard_xlarge"),
		LANDSCAPE_S("landscape_small"),
		LANDSCAPE_M("landscape_medium"),
		LANDSCAPE_L("landscape_large"),
		LANDSCAPE_XL("landscape_xlarge");

		private final String prefix;

		ImageSize(String prefix) {
			this.prefix = prefix;
		}
	}

	private final String urlBase;
	private final String apiKey;
	private final String privateKey;
	private final OkHttpClient client;

	/**
	 * class representing the main wrapper of a successful character response from the Marvel API
	 */
	public static class CharacterResponse {
		int code;
		String status;
		CharacterContainer data;
		String copyright;
		String attributionText;
		String attributionHTML;
	}

	/**
	 * class representing the main container of a successful character response from the Marvel API
	 */
	public static class CharacterContainer {
		int offset;
		int limit;
		int total;
		int count;
		ArrayList<Character> results = new ArrayList<>();
	}

	/**
	 * class representing a reference to a character in the response from the Marvel API
	 */
	public static class Character {
		public Character() {
			id = 0;
			name = "";
			description = "";
			resourceURI = "";
			thumbnail = null;
		}
		int id;
		String name;
		String description;
		String resourceURI;
		Image thumbnail;
	}

	/**
	 * class representing a URL in the response from the Marvel API
	 */
	public static class URL {
		String type;
		String url;
	}

	/**
	 * class representing a piece of text in the response from the Marvel API
	 */
	public static class Text {
		String type;
		String language;
		String text;
	}

	/**
	 * class representing a reference to an image resource in the response from the Marvel API
	 */
	public static class Image {
		String path;
		String extension;

		public Image(String path, String extension) {
			this.path = path;
			this.extension = extension;
		}

		public Image() {
			this("", "");
		}

		String getURL(ImageSize size) {
			return path + "/" + size.prefix + "." + extension;
		}
	}

	/**
	 * class representing an error response in the response from the MMarvel API
	 */
	public static class ErrorResponse {
		String code;
		String message;
	}

	/**
	 *  class constructor
	 * @param urlBase
	 * @param apiKey
	 * @param privateKey
	 */
	public MarvelAPI(final String urlBase, final String apiKey, final String privateKey)
	{
		this.urlBase = urlBase;
		this.apiKey = apiKey;
		this.privateKey = privateKey;
		client = new OkHttpClient();
	}

	/**
	 * create the basic observable of an okhttp response
	 * @param okRequest
	 * @return
	 */
	@NonNull
	private Observable<Response> createObservable(final Request okRequest) {
		return Observable.create(new Observable.OnSubscribe<Response>() {

			@Override
			public void call(Subscriber<? super Response> subscriber) {
				Log.d("MarvelAPI", "created observervable on "+okRequest.toString()+" on "+Thread.currentThread().getId());
				try {
					Response response = client.newCall(okRequest).execute();
					subscriber.onNext(response);
					if (!response.isSuccessful()) {
						subscriber.onError(new Exception("error"));
					} else {
						subscriber.onCompleted();
					}
				} catch (IOException e) {
					subscriber.onError(e);
				}
			}
		});
	}

	/**
	 * do the basic mapping of an okhttp response into a string ... we're assuming the amount of data
	 * is reasonable (which it is for the marvel api), so .string() is a reasonable way to grab all data
	 */
	private Func1<Response, String> responseBodyMapper = new Func1<Response, String>() {
		@Override
		public String call(Response response) {
			int responseCode = response.code();
			Log.d("MarvelAPI", "got response, code "+responseCode);
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new RuntimeException("Bad response code "+responseCode);
			}
			String responseBody = "";
			try {
				responseBody = response.body().string();
				response.body().close();
			} catch (IOException e) {
				response.body().close();
				throw new RuntimeException("IO Exception getting response body"+e.getMessage());
			} catch (Exception e) {
				response.body().close();
				throw new RuntimeException("Unexpected Exception getting response body "+e.getClass().toString());
			}

			// we try to convert the response body to an ErrorResponse message. if we can, we throw this as an error
			// otherwise, we let the chaing continue
			Gson gson = new GsonBuilder().create();
			ErrorResponse errorResponse = gson.fromJson(responseBody, ErrorResponse.class);
			if (errorResponse != null && errorResponse.code != null && errorResponse.message != null) {
				throw new RuntimeException("Error status from server: "+errorResponse.message);
			}
			return responseBody;
		}

	};

	/**
	 * main api call to return a character response. if this completes, a character response will be Observable,
	 * otherwise an error response will come back via a RuntimeException and be viewed in the observers Error handler
	 * @param okRequest
	 * @return
	 */
	protected Observable<CharacterResponse> getCharacters(Request okRequest) {
		Observable<Response> observable = createObservable(okRequest);
		return observable
				.subscribeOn(Schedulers.newThread())
				.map(responseBodyMapper)
				.map(new Func1<String, CharacterResponse>() {
					@Override
					public CharacterResponse call(String responseBody) {
						return parseCharacterResponse(responseBody);
					}
				})
				.observeOn(AndroidSchedulers.mainThread());
	}

	/**
	 * call to get the default page of characters
	 * @return
	 */
	public Observable<CharacterResponse> getCharacters() {
		return getCharacters(characterRequest(-1, -1, null, false));
	}

	/**
	 * call to get the page of characters defined by limit and offset
	 * @param limit
	 * @param offset
	 * @return
	 */
	public Observable<CharacterContainer> getCharacters(int limit, int offset) {
		return getCharacters(characterRequest(limit, offset, null, false))
				.map(new Func1<CharacterResponse, CharacterContainer>() {
					@Override
					public CharacterContainer call(CharacterResponse characterResponse) {
						return characterResponse.data;
					}
				});
	}

	/**
	 * call to get the character named exactly 'name'
	 * @param name
	 * @return
	 */
	public Observable<Character> getCharacter(final String name) {
		return getCharacters(characterRequest(-1, -1, name, false))
				.map(new Func1<CharacterResponse, Character>() {
					@Override
					public Character call(CharacterResponse characterResponse) {
						if (characterResponse.data.results.size() == 0) {
							throw new RuntimeException("Character matching "+name+" not found");
						}
						return characterResponse.data.results.get(0);
					}
				});
	}

	/**
	 * call to get a list of characters whose name starts with name
	 * @param name
	 * @return
	 */
	public Observable<ArrayList<Character>> getCharacterMatching(String name) {
		return getCharacters(characterRequest(-1, -1, name, true))
				.map(new Func1<CharacterResponse, ArrayList<Character>>() {
					@Override
					public ArrayList<Character> call(CharacterResponse characterResponse) {
						return characterResponse.data.results;
					}
				});
	}

	/**
	 * call to get a list of characters whose name starts with name
	 * @param limit
	 * @param offset
	 * @param name
	 * @return
	 */
	public Observable<CharacterResponse> getCharacterMatching(String name, int limit, int offset) {
		return getCharacters(characterRequest(limit, offset, name, true));
	}

	/**
	 * process a character request response
	 * @param responseBody
	 * @return
	 */
	@NonNull
	public static final CharacterResponse parseCharacterResponse(String responseBody) {
		Gson gson = new GsonBuilder().create();
		CharacterResponse characters = gson.fromJson(responseBody, CharacterResponse.class);
		if (characters == null) {
			throw new RuntimeException("Unexpected null result processing JSON");
		}
		return characters;
	}

	/**
	 *  builds an appropriate okhttp request object to fetch characters
	 * @param limit
	 * @param offset
	 * @param name
	 * @param startsWith
	 * @return
	 */
	public Request characterRequest(int limit, int offset, String name, boolean startsWith) {
		HttpUrl okurl = HttpUrl.parse(urlBase);
		if (okurl == null) {
			throw new RuntimeException("request builder fails on ");
		}
		final Long tsLong = System.currentTimeMillis()/1000;
		final String ts = tsLong.toString();
		final String hash = md5(ts + privateKey + apiKey);
		HttpUrl.Builder b = okurl.newBuilder()
				.addEncodedPathSegment("characters")
				.addQueryParameter("apikey", apiKey)
				.addQueryParameter("ts", ts)
				.addQueryParameter("hash", hash);

		if (limit > 0) {
			b.addQueryParameter("limit", Integer.toString(limit));
		}
		if (offset > 0) {
			b.addQueryParameter("offset", Integer.toString(offset));
		}
		if (name != null) {
			b.addQueryParameter(startsWith? "nameStartsWith":"name", name);
		}
		okurl = b.build();
//		Log.d("fetchRequest", "made url!! "+okurl.toString());
		return new Request.Builder().url(okurl).build();
	}

	/**
	 * return the md5 hash of a given string. ideally we don't reinvent the wheel on simple key algorithms.
	 * apache commons codec to the rescue.
	 * @param s
	 * @return
	 */
	public static final String md5(final String s) {
		if (s == null) {
			return "";
		}
		return new String(Hex.encodeHex(DigestUtils.md5(s)));
		/*
		final String MD5 = "MD5";
		try {
			MessageDigest digest = MessageDigest.getInstance(MD5);
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuilder hexString = new StringBuilder();
			for (byte aMessageDigest : messageDigest) {
				String h = Integer.toHexString(0xFF & aMessageDigest);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
		*/
	}


}
