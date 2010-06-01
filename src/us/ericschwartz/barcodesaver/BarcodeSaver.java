package us.ericschwartz.barcodesaver;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class BarcodeSaver extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    
    private static int mLastBarcodeId = -1;
	
	private BarcodeDbAdapter mDbHelper;
	private AlertDialog mZxingAlertDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcode_list);
        mDbHelper = new BarcodeDbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }
    
    private void fillData() {
        // Get all of the rows from the database and create the item list
    	Cursor barcodesCursor = mDbHelper.fetchAllBarcodes();
        startManagingCursor(barcodesCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{BarcodeDbAdapter.KEY_NAME, BarcodeDbAdapter.KEY_FORMAT};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1, R.id.text2};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
        	    new SimpleCursorAdapter(this, R.layout.barcode_row, barcodesCursor, from, to);
        setListAdapter(notes);
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Cursor c = mDbHelper.fetchBarcode(id);
		String format = c.getString(
			c.getColumnIndexOrThrow(BarcodeDbAdapter.KEY_FORMAT));
		String value = c.getString(
		    c.getColumnIndexOrThrow(BarcodeDbAdapter.KEY_VALUE));
		if(format.equals("UPC_A")) {
			format = "EAN_13";
			value = '0' + value;
		}
		IntentIntegrator.shareText(this, value, format);
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(Menu.NONE, DELETE_ID, Menu.NONE, R.string.menu_delete);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean retVal = super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, INSERT_ID, Menu.NONE, R.string.menu_insert);
		return retVal;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
			mDbHelper.deleteBarcode(info.id);
			fillData();
			return true;			
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case INSERT_ID:
			mZxingAlertDialog = IntentIntegrator.initiateScan(this);
		}
		return super.onMenuItemSelected(featureId, item);
	}
    
	@Override
	protected void onPause() {
		if (mZxingAlertDialog != null) {
			mZxingAlertDialog.dismiss();
		}
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		fillData();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			mDbHelper.createBarcode("Barcode " + (++mLastBarcodeId),
									scanResult.getContents(),
									scanResult.getFormatName());
		 }
		fillData();
	}    
}