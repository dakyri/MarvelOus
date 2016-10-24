package com.mayaswell.marvelous;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mayaswell.marvelous.MarvelAPI.Character;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by dak on 10/23/2016.
 */
public class CharacterAdapter  extends RecyclerView.Adapter<CharacterAdapter.ViewHolder>{
	ArrayList<Character> dataSet = new ArrayList<Character>();

	public class ViewHolder  extends RecyclerView.ViewHolder {
		public RelativeLayout parent;
		protected TextView nameView;
		protected TextView descView;
		public ViewHolder(RelativeLayout v) {
			super(v);
			parent = v;
			nameView = (TextView) v.findViewById(R.id.itemNameView);
			descView = (TextView) v.findViewById(R.id.itemDescView);
		}

		public void setToCharacter(Character c) {
			nameView.setText(c.name);
			descView.setText(c.description);
			parent.setTag(c);
			Log.d("CharAdapter", "setting "+c.name+c.description);
		}
	}

	public void clear() {
		dataSet.clear();
		notifyDataSetChanged();
	}

	public void addAll(Collection<Character> list) {
		dataSet.clear();
		dataSet.addAll(list);
		notifyDataSetChanged();
	}


	/**
	 *   create a new view
	 * @param parent
	 * @param viewType
	 * @return
	 */
	@Override
	public CharacterAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.character_list_item, parent, false);
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					RelativeLayout rl = (RelativeLayout) v;
					Character c = (Character) rl.getTag();
					MainActivity ml = (MainActivity) rl.getContext();
					ml.showDetailView(c);
					ml.updateCharacter(c);
				} catch (ClassCastException e) { // should not happen here

				}
			}
		});
		ViewHolder vh = new ViewHolder(v);
		return vh;
	}

	@Override
	public void onBindViewHolder(CharacterAdapter.ViewHolder holder, int position) {
		Log.d("CharAdapter", "binding at "+position);
		holder.setToCharacter(dataSet.get(position));
	}

	@Override
	public int getItemCount() {
		return dataSet.size();
	}

}
