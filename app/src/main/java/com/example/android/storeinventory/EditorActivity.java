package com.example.android.storeinventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.storeinventory.data.InventoryContract.InventoryEntry;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String TAG = EditorActivity.class.getSimpleName();

    private TextView mItemName;
    private TextView mItemDesc;
    private TextView mItemQuantity;
    private TextView mItemPrice;

    private Uri mCurrentItemUri;

    private boolean mItemHasChanged = false;

    private static final int ITEM_EDITOR_LOADER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        Uri currentItemUri = intent.getData();

        Log.d(TAG, "onCreate: currentItemUri :: "+currentItemUri);

        // Find all relevant views that we will need to read user input from
        mItemName = (EditText) findViewById(R.id.edit_item_name);
        mItemDesc = (EditText) findViewById(R.id.edit_item_desc);
        mItemQuantity = (EditText) findViewById(R.id.edit_item_quantity);
        mItemPrice = (EditText) findViewById(R.id.edit_item_price);

        if(currentItemUri == null){
            setTitle(getString(R.string.editor_activity_title_new_item));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        }else{
            setTitle(getString(R.string.editor_activity_title_edit_item));
            mCurrentItemUri = currentItemUri;
            getLoaderManager().initLoader(ITEM_EDITOR_LOADER, null, this);
        }

        mItemName.setOnTouchListener(mTouchListener);
        mItemDesc.setOnTouchListener(mTouchListener);
        mItemQuantity.setOnTouchListener(mTouchListener);
        mItemPrice.setOnTouchListener(mTouchListener);

        Log.d(TAG, "onCreate: ended");
    }

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the mItemHasChanged boolean to true.

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: started");
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        Log.d(TAG, "onCreateOptionsMenu: ended");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: started");
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                if(null == mCurrentItemUri){
                    // Save item to database if the flow is "Add an Item"
                    insertItem();
                }else{
                    // Update the pet to database if the flow is "Edit Item"
                    updateItem();
                }
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        Log.d(TAG, "onOptionsItemSelected: ended");
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
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
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /*
     *Get user input from editor and save new item into database
     */
    private void insertItem(){
        Log.d(TAG, "insertItem: started");
        ContentValues values = readUserInput();

        if (null == mCurrentItemUri) {
            if(TextUtils.isEmpty(values.getAsString(InventoryEntry.COLUMN_ITEM_NAME)) && TextUtils.isEmpty(values.getAsString(InventoryEntry.COLUMN_ITEM_DESC)) && TextUtils.isEmpty(values.getAsString(InventoryEntry.COLUMN_ITEM_QUANTITY)) && TextUtils.isEmpty(values.getAsString(InventoryEntry.COLUMN_ITEM_PRICE))){
                return;
            }
        }

        // Insert a new item into the provider, returning the content URI for the new pet.
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
        Log.d(TAG, "insertItem: newUri :: "+newUri);
        // Show a toast message depending on whether or not the insertion was successful
        if(newUri == null){
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_item_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_item_successful), Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "insertItem: ended");
    }

    /*
     *Get user input from editor and update changes into database
     */
    private void updateItem(){
        Log.d(TAG, "updateItem: started");
        ContentValues values = readUserInput();

        // Update the pet into the provider, returning the content URI for the pet.
        int noOfRowsUpdated = getContentResolver().update(mCurrentItemUri, values, null, null);
        Log.d(TAG, "updateItem: noOfRowsUpdated :: "+noOfRowsUpdated);
        // Show a toast message depending on whether or not the update was successful
        if(noOfRowsUpdated < 1){
            // If the new content URI is null, then there was an error with update.
            Toast.makeText(this, getString(R.string.editor_update_item_failed), Toast.LENGTH_SHORT).show();
        }else{
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_update_item_successful), Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "updateItem: ended");
    }

    private ContentValues readUserInput(){
        Log.d(TAG, "readUserInput: started");
        ContentValues values = new ContentValues();
        // Read from input fields
        // Use trim to eliminate leading or trailing white spaces
        String nameString, descString;
        int quantity, price;
        if(null != mItemName.getText()) {
            nameString = mItemName.getText().toString().trim();
            values.put(InventoryEntry.COLUMN_ITEM_NAME, nameString);
        }
        if(null != mItemDesc.getText()) {
            descString = mItemDesc.getText().toString().trim();
            values.put(InventoryEntry.COLUMN_ITEM_DESC, descString);
        }
        String quantityString = mItemQuantity.getText().toString().trim();
        if(!TextUtils.isEmpty(quantityString)){
            quantity = Integer.parseInt(quantityString);
            values.put(InventoryEntry.COLUMN_ITEM_QUANTITY, quantity);
        }else{
            values.put(InventoryEntry.COLUMN_ITEM_QUANTITY, 0);
        }
        String priceString = mItemQuantity.getText().toString().trim();
        if(!TextUtils.isEmpty(priceString)){
            price = Integer.parseInt(priceString);
            values.put(InventoryEntry.COLUMN_ITEM_PRICE, price);
        }else{
            values.put(InventoryEntry.COLUMN_ITEM_PRICE, 0);
        }

        Log.d(TAG, "readUserInput: ended");
        return values;
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
