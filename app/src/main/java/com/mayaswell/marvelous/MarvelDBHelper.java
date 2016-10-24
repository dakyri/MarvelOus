package com.mayaswell.marvelous;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.mayaswell.marvelous.MarvelAPI.Character;
import com.mayaswell.marvelous.MarvelAPI.Image;

/**
 * Created by dak on 10/23/2016.
 */
public class MarvelDBHelper extends SQLiteOpenHelper {
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "marvelCache";

	public static final String TABLE_CHARACTERS = "characters";

	public static final String KEY_DBKEY = "dbkey";
	public static final String KEY_ID = "id";
	public static final String KEY_NAME = "name";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_IMAGE_PATH = "image_path";
	public static final String KEY_IMAGE_EXTENSION = "image_suffix";
	public static final String KEY_TIMESTAMP = "timestamp";
	private final int maxCharacters;

	public MarvelDBHelper(int maxCharacters, Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.maxCharacters = maxCharacters;
	}

	/**
	 * create our cache db, with one table, for characters
	 * @param db
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("db helper", "creating db");
		db.execSQL(
			"CREATE TABLE " + TABLE_CHARACTERS + "(" +
				KEY_DBKEY + " INTEGER PRIMARY KEY" + "," +
				KEY_ID + " INTEGER" + "," +
				KEY_NAME + " TEXT" + ","+
				KEY_DESCRIPTION + " TEXT" + "," +
				KEY_IMAGE_PATH + " TEXT" + "," +
				KEY_IMAGE_EXTENSION + " TEXT" + "," +
				KEY_TIMESTAMP + " INTEGER" +
				")");
	}

	/**
	 * upgrade the db ... just delete current tables and rebuild .. it's not a heavy cache here
	 * @param db
	 * @param oldVersion
	 * @param newVersion
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHARACTERS);
		onCreate(db);
	}

	/**
	 * insert character data into our default db. checks to ensure we don't double up on id
	 * @param character
	 * @return true if inserted
	 */
	public boolean createCharacter(Character character) {
		int key = getKey4Id(character.id);
		if (key >= 0) {
			return false;
		}
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = values4Character(character);
		db.insert(TABLE_CHARACTERS, null, values);
		db.close();
		return true;
	}

	public Character getCharacter4Id(long id)
	{
		SQLiteDatabase db = getReadableDatabase();
		String selectQuery = "SELECT  * FROM " + TABLE_CHARACTERS + " WHERE " + KEY_ID + " = " + id;
		Cursor c = db.rawQuery(selectQuery, null);
		if (c.getCount() == 0) {
			return null;
		}
		Character ch = characterAtCursor(c);
		db.close();
		return ch;
	}

	public int getKey4Id(long id)
	{
		SQLiteDatabase db = getReadableDatabase();
		String selectQuery = "SELECT  * FROM " + TABLE_CHARACTERS + " WHERE " + KEY_ID + " = " + id;
		Cursor c = db.rawQuery(selectQuery, null);
		if (c.getCount() == 0) {
			return -1;
		}
		c.moveToFirst();
		int i = c.getInt(c.getColumnIndex(KEY_DBKEY));
		db.close();
		return i;
	}

	/**
	 * delete the character at the given primary key position
	 * @param key
	 */
	public void deleteCharacter4Key(long key)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_CHARACTERS, KEY_DBKEY + " = ?", new String[] { String.valueOf(key) });
		db.close();
	}

	/**
	 * get the list of cached characters, no limits
	 * @return
	 */
	public List<Character> getCharacters() {
		return getCharacters(0);
	}

	/**
	 * get the list of cached characters, up to the given numberic limit, sorted in descending order of timestamp
	 * @param limit
	 * @return
	 */
	public List<Character> getCharacters(int limit)
	{
		List<Character> ql = new ArrayList<Character>();
		String selectQuery = "SELECT  * FROM " + TABLE_CHARACTERS + " ORDER BY " + KEY_TIMESTAMP + " DESC ";
		if (limit > 0) {
			selectQuery += " LIMIT " + limit;
		}
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery(selectQuery, null);

		Log.d("db helper", selectQuery+" --> "+c.getCount()+" rows");

		if (c.moveToFirst()) {
			do {
				ql.add(characterAtCursor(c));
			} while (c.moveToNext());
		}
		db.close();
		return ql;
	}

	/**
	 * construct a character for the data at the current position of the given cursor
	 * @param c
	 * @return
	 */
	@NonNull
	private Character characterAtCursor(Cursor c) {
		Character character = new Character();
		character.id = c.getInt(c.getColumnIndex(KEY_ID));
		character.name = (c.getString(c.getColumnIndex(KEY_NAME)));
		character.description = (c.getString(c.getColumnIndex(KEY_DESCRIPTION)));
		character.thumbnail = new Image(
				c.getString(c.getColumnIndex(KEY_IMAGE_PATH)),
				c.getString(c.getColumnIndex(KEY_IMAGE_EXTENSION)));
		return character;
	}

	/**
	 * construct a ContentValues object from the given character data
	 * @param character
	 * @return
	 */
	@NonNull
	private ContentValues values4Character(Character character) {
		ContentValues values = new ContentValues();
		values.put(KEY_ID, character.id);
		values.put(KEY_NAME, character.name);
		values.put(KEY_DESCRIPTION, character.description);
		values.put(KEY_IMAGE_PATH, character.thumbnail.path);
		values.put(KEY_IMAGE_EXTENSION, character.thumbnail.extension);
		values.put(KEY_TIMESTAMP, System.currentTimeMillis());
		return values;
	}

	/**
	 * updates the character in the default database, inserting it if it doesn't already exist
	 * @param character
	 * @return true if we are adding for the first time
	 */
	public boolean updateCharacter(Character character)
	{
		int key = getKey4Id(character.id);
		if (key < 0) {
			createCharacter(character);
			Log.d("MarvelDBHelper", "creating new entry for "+character.name);
			return true;
		}
		Log.d("MarvelDBHelper", "updating entry for "+character.name + " at key "+key);

		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = values4Character(character);
		Log.d("MarvelDBHelper", "update values "+values.toString());
		int nRows = db.update(TABLE_CHARACTERS, values, KEY_DBKEY + " = ?", new String[] { String.valueOf(key) });
		if (nRows != 1) {
			Log.d("MarvelDBHelper", "update failed "+nRows);
		}
		db.close();
		return false;
	}

	/**
	 * trims to the most recent "limit" number of entries. Actually does this by deleting anything older than limit-th
	 * oldest entry in the db. If there are any entries with the same timestamp, they would also be kept ... this is
	 * incredibly unlikely, but won't in any case affect the functioning of the cache.
	 *
	 * @param limit
	 * @return
	 */
	public boolean trimToNewest(int limit) {
		SQLiteDatabase db = getReadableDatabase();
		db.delete(TABLE_CHARACTERS,
				KEY_TIMESTAMP + " < " +
						"(" + "SELECT " + KEY_TIMESTAMP + " FROM "+ TABLE_CHARACTERS +
							" ORDER BY " + KEY_TIMESTAMP + " DESC " + " LIMIT 1 OFFSET " + limit + ")",
				new String[] {}
		);
		db.close();
		return true;
	}

	/**
	 * trims db to the default amount
	 * @return
	 */
	public boolean trimToNewest() {
		return trimToNewest(maxCharacters);
	}

}
