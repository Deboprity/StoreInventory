package com.example.android.storeinventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.storeinventory.data.InventoryContract;
import com.example.android.storeinventory.data.InventoryContract.InventoryEntry;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = DetailsActivity.class.getSimpleName();

    private TextView mItemName;
    private TextView mItemDesc;
    private TextView mItemQuantity;
    private TextView mItemPrice;
    private String mItemImage;

    private Uri mCurrentItemUri;

    private static final int ITEM_DETAILS_LOADER = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Intent intent = getIntent();
        Uri currentItemUri = intent.getData();

        Log.d(TAG, "onCreate: currentItemUri :: "+currentItemUri);

        mItemName = findViewById(R.id.item_name);
        mItemDesc = findViewById(R.id.item_desc);
        mItemQuantity = findViewById(R.id.item_quantity);
        mItemPrice = findViewById(R.id.item_price);

        if(null != currentItemUri){
            mCurrentItemUri = currentItemUri;
            getLoaderManager().initLoader(ITEM_DETAILS_LOADER, null, this);
        }

        Log.d(TAG, "onCreate: ended");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: started");
        // Inflate the menu options from the res/menu/menu_details.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_details, menu);
        Log.d(TAG, "onCreateOptionsMenu: ended");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: started");
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_edit_entry:
                if(null != mCurrentItemUri){
                    Intent intent = new Intent(DetailsActivity.this, EditorActivity.class);
                    Log.d(TAG, "onItemClick: currentItemUri :: "+mCurrentItemUri);
                    intent.setData(mCurrentItemUri);
                    startActivity(intent);
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete_entry:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
               NavUtils.navigateUpFromSameTask(DetailsActivity.this);
               return true;
        }
        Log.d(TAG, "onOptionsItemSelected: ended");
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue viewing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        Log.d(TAG, "deleteItem: started");
        // Delete the item from the provider, returning the number of rows deleted.
        int noOfRowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);
        Log.d(TAG, "deleteItem: noOfRowsDeleted :: "+noOfRowsDeleted);
        // Show a toast message depending on whether or not the delete was successful
        if(noOfRowsDeleted < 1){
            // If the noOfRowsDeleted is less than 1, then there was an error with delete.
            Toast.makeText(this, getString(R.string.editor_delete_item_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_item_successful), Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "deleteItem: ended");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader: started");
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the pet table
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_ITEM_NAME,
                InventoryEntry.COLUMN_ITEM_DESC,
                InventoryEntry.COLUMN_ITEM_QUANTITY,
                InventoryEntry.COLUMN_ITEM_PRICE };
        Log.d(TAG, "onCreateLoader: projection :: "+projection.length);
        Log.d(TAG, "onCreateLoader: mCurrentItemUri :: "+mCurrentItemUri);
        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "onLoadFinished: started");
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
            int descColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_DESC);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String description = cursor.getString(descColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);

            // Update the views on the screen with the values from the database
            mItemName.setText(name);
            mItemDesc.setText(description);
            mItemQuantity.setText(Integer.toString(quantity));
            mItemPrice.setText(Integer.toString(price));
        }
        Log.d(TAG, "onLoadFinished: ended");
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
