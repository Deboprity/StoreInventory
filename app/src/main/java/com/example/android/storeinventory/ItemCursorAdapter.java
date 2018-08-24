package com.example.android.storeinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.storeinventory.data.InventoryContract.InventoryEntry;


/**
 * {@link ItemCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of pet data as its data source. This adapter knows
 * how to create list items for each row of pet data in the {@link Cursor}.
 */
public class ItemCursorAdapter extends CursorAdapter {


    private static final String TAG = ItemCursorAdapter.class.getSimpleName();
    private static final String empty_space = " ";
    private Uri mCurrentItemUri;
    /**
     * Constructs a new {@link ItemCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.item_name);
        TextView descTextView = (TextView) view.findViewById(R.id.item_desc);
        TextView quantityTextView = (TextView) view.findViewById(R.id.item_quantity);
        Button saleButton = (Button)view.findViewById(R.id.sale_button);
        // Find the columns of item attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(InventoryEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
        int descColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_DESC);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANTITY);
        // Read the item attributes from the Cursor for the current item
        final String id = cursor.getString(idColumnIndex);
        String itemName = cursor.getString(nameColumnIndex);
        String itemDesc = cursor.getString(descColumnIndex);
        final String itemQuantity = cursor.getString(quantityColumnIndex);
        //Adding the quantity label before the quantity
        String itemQuantityString = context.getString(R.string.quantity_label) + empty_space + itemQuantity;
        // If the item description is empty string or null, then use some default text
        // that says "Unknown description", so the TextView isn't blank.
        if (TextUtils.isEmpty(itemDesc)) {
            itemDesc = context.getString(R.string.unknown_desc);
        }
        // Update the TextViews with the attributes for the current item
        nameTextView.setText(itemName);
        descTextView.setText(itemDesc);
        quantityTextView.setText(itemQuantityString);

        final ContentValues values = new ContentValues();

        // implementing onClick for Sale Button
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "Sale Button is clicked for " + id, Toast.LENGTH_SHORT).show();
                Uri currentItemUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, Long.parseLong(id));
                Log.d(TAG, "onClick: currentItemUri :: "+currentItemUri);
                if(null != currentItemUri){
                    mCurrentItemUri = currentItemUri;
                    int quantity = 0;
                    try{
                        quantity = Integer.parseInt(itemQuantity);
                        // If the item quantity is greater than 0, then only Sale button will be visible
                        if(quantity > 0){
                            // Decreasing the quantity of the item by 1
                            quantity = quantity - 1;
                            values.put(InventoryEntry.COLUMN_ITEM_QUANTITY, quantity);
                            updateItemQuantity(context, values);
                        }else{
                            Toast.makeText(context, context.getText(R.string.not_sufficient_sale_item), Toast.LENGTH_SHORT).show();
                        }
                    }catch (NumberFormatException e){
                        Toast.makeText(context, "NumberFormatException", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }

    /*
     *GReduces the item quantity by 1 and changes into database
     */
    private void updateItemQuantity(Context context, ContentValues values){
        Log.d(TAG, "updateItemQuantity: started");
        // Update the pet into the provider, returning the content URI for the pet.
        int noOfRowsUpdated = context.getContentResolver().update(mCurrentItemUri, values, null, null);
        Log.d(TAG, "updateItemQuantity: noOfRowsUpdated :: "+noOfRowsUpdated);
        // Show a toast message depending on whether or not the update was successful
        if(noOfRowsUpdated < 1){
            // If the new content URI is null, then there was an error with update.
            Toast.makeText(context, context.getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(context, context.getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "updateItemQuantity: ended");
    }

}