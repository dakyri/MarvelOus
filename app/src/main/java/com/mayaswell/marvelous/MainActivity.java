package com.mayaswell.marvelous;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.bumptech.glide.Glide;
import com.mayaswell.marvelous.MarvelAPI.Character;
import com.mayaswell.marvelous.MarvelAPI.ImageSize;

import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

	private MarvelAPI marvelAPI;
	private MarvelDBHelper cachedDB;
	private CharacterAdapter characterAdapter;
	private RecyclerView cacheListView;
	private LinearLayoutManager characterListLayoutManager;
	private ViewAnimator viewAnimator;
	private RelativeLayout searchView;
	private EditText nameView;
	private Button goButton;
	private ScrollView detailView;
	private TextView statusView;
	private String currentSearchText = "";
	private TextView detailNameView;
	private ImageView detailImageView;
	private TextView detailDescriptionView;
	private List<Character> cachedRequests = null;
	private int maxCharactersCached;
	private ProgressBar progressBar;

	private Properties properties;
	private String apiKey;
	private String privateKey;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);
		AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(2000);
		viewAnimator.setInAnimation(animation);
		animation = new AlphaAnimation(1.0f, 0.0f);
		animation.setDuration(2000);
		viewAnimator.setOutAnimation(animation);

		// componentts of search view
		searchView = (RelativeLayout) findViewById(R.id.searchView);
		nameView = (EditText) findViewById(R.id.nameView);
		goButton = (Button) findViewById(R.id.goButton);
		goButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showProgress(true);
				clearError();
				currentSearchText = nameView.getText().toString();
				marvelAPI.getCharacterMatching(currentSearchText).subscribe(new Subscriber<ArrayList<Character>>() {
					@Override
					public void onCompleted() {
						showProgress(false);
						Log.d("Main", "Results are ok");
					}

					@Override
					public void onError(Throwable e) {
						showError("API Error", e.getMessage());
						showProgress(false);
						e.printStackTrace();
					}

					@Override
					public void onNext(ArrayList<Character> characters) {
						onCharacterListReceived(characters);
					}
				});
			}
		});
		statusView = (TextView) findViewById(R.id.statusView);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		characterAdapter = new CharacterAdapter();
		cacheListView = (RecyclerView) findViewById(R.id.cacheListView);
		characterListLayoutManager = new LinearLayoutManager(this);
		cacheListView.setLayoutManager(characterListLayoutManager);
		cacheListView.setAdapter(characterAdapter);

		// componentts of detail view
		detailView = (ScrollView) findViewById(R.id.detailView);
		detailNameView = (TextView)findViewById(R.id.detailNameView);
		detailImageView = (ImageView) findViewById(R.id.detailImageView);
		detailDescriptionView = (TextView) findViewById(R.id.detailDescriptionView);

		properties = getProperties("marvel.properties");
		if (properties != null) {
			apiKey = properties.getProperty("ApiKey", getResources().getString(R.string.api_key));
			privateKey = properties.getProperty("PrivateKey", getResources().getString(R.string.private_key));
			Log.d("MainActivity", "Got properties from assets ok");
		} else {
			apiKey = getResources().getString(R.string.api_key);
			privateKey = getResources().getString(R.string.private_key);
		}
		// other bits that we might want
		marvelAPI = new MarvelAPI( getResources().getString(R.string.marvel_url_base), apiKey, privateKey );
		maxCharactersCached = getResources().getInteger(R.integer.maxCharactersCached);
		cachedDB = new MarvelDBHelper(maxCharactersCached, this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		cachedRequests = cachedDB.getCharacters(maxCharactersCached);
		Log.d("MainActivity", "found "+cachedRequests.size()+ " cached requests");
		characterAdapter.addAll(cachedRequests);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d("Main", "got intent " + intent.toString());
		super.onNewIntent(intent);
	}

	@Override
	public void onBackPressed() {
		if (viewAnimator.getDisplayedChild() == viewAnimator.indexOfChild(detailView)) {
			showSearchView();
		} else {
			super.onBackPressed();
		}
	}

	/**
	 * take appropriate actions for the arrival of a character list. Currently only the first character is shown
	 * @param characters
	 */
	private void onCharacterListReceived(ArrayList<Character> characters) {
		if (characters == null || characters.size() == 0) {
			showError("Not found", "Name "+currentSearchText+" not found");
		} else {
			Character c = characters.get(0);
			showDetailView(c);
			updateCharacter(c);
		}
	}

	/**
	 * show our main detail view, filled in for the given character
	 * @param c
	 */
	public void showDetailView(Character c) {
		// hide the keyboard with extreme prejudice
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}

		Context context = detailImageView.getContext();
		Glide.with(context).load(c.thumbnail.getURL(ImageSize.PORTRAIT_XL)).into(detailImageView);
		Log.d("MainActivity", "got character "+c.name+", "+c.description);
		detailNameView.setText(c.name);
		detailDescriptionView.setText(c.description);
		viewAnimator.setDisplayedChild(viewAnimator.indexOfChild(detailView));
	}

	/**
	 * show our main openning page with search button and text input
	 */
	public void showSearchView() {
		viewAnimator.setDisplayedChild(viewAnimator.indexOfChild(searchView));
	}

	/**
	 * display an error message
	 * @param title
	 * @param message
	 */
	protected void showError(String title, String message) {
		// hide the keyboard with extreme prejudice
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}

		statusView.setText(message);
		viewAnimator.setDisplayedChild(viewAnimator.indexOfChild(searchView));
	}

	/**
	 * hide the current error message
	 */
	private void clearError() {
		statusView.setText("");
	}

	/**
	 * refresh the database cache for the given character
	 * @param c
	 */
	public void updateCharacter(Character c) {
		if (cachedDB.updateCharacter(c)) {
			cachedDB.trimToNewest();
			cachedRequests = cachedDB.getCharacters(maxCharactersCached);
			characterAdapter.addAll(cachedRequests);
		}
	}

	/**
	 * show/hide the current progress bar
	 * @param b
	 */
	private void showProgress(boolean b) {
//		setProgressBarVisibility(b);
//		setProgressBarIndeterminateVisibility(b);
		if (progressBar != null) {
			progressBar.setVisibility(b?View.VISIBLE:View.GONE);
		}
	}

	/**
	 * load the given properties file from assets
	 * @param FileName
	 * @return
	 */
	public Properties getProperties(String FileName) {
		Properties p = new Properties();
		try {
			p = new Properties();
			p.load(getAssets().open(FileName));
		} catch (IOException e) {
			Log.e("Main Activity", "Error getting assets: "+e.toString());
		}
		return p;
	}
}
