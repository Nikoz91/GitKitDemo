package it.polimi.frontend.util;
import it.polimi.appengine.entity.manager.Manager;
import it.polimi.appengine.entity.manager.model.Feedback;
import it.polimi.appengine.entity.manager.model.Request;
import it.polimi.appengine.entity.manager.model.User;
import it.polimi.frontend.activity.CloudEndpointUtils;
import it.polimi.frontend.activity.LoginSession;
import it.polimi.frontend.activity.MyApplication;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;

public class QueryManager {

	private List<User> users;
	private List<Feedback> feedback;
	private List<Request> requests;
	private List<OnRequestLoadedListener> listeners;
	private Manager manager;
	private static QueryManager instance;
	private ProgressDialog mProgressDialog;
	/** 
	 * Metodi per mostrare o meno il progressDialog
	 * */
	protected void showDialog() {
		if (mProgressDialog == null) {
			setProgressDialog();
		}
		mProgressDialog.show();
	}

	protected void hideDialog() {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
	}

	private void setProgressDialog() {
		mProgressDialog = new ProgressDialog(MyApplication.getContext());
		mProgressDialog.setTitle("Attendi...");
		mProgressDialog.setMessage("Sto scaricando...");
	}
	
	
	
	private QueryManager(){
		this.users = new ArrayList<User>();
		this.requests = new ArrayList<Request>();
		this.feedback = new ArrayList<Feedback>();
		this.listeners = new ArrayList<OnRequestLoadedListener>();
		Manager.Builder managerBuilder = new Manager.Builder(
				AndroidHttp.newCompatibleTransport(), new JacksonFactory(), null);
		managerBuilder = CloudEndpointUtils.updateBuilder(managerBuilder);
		this.manager = managerBuilder.build();
	}

	public static QueryManager getInstance(){
		if (instance==null){
			instance = new QueryManager();
		}
		return instance;
	}

	public void loadData(){
		new LoadDataTask().execute();
	}
	
	public void loadRequest(){
		System.out.println("Mi connetto per scaricare le richieste...");
		new LoadDataTask().execute();
	}

	public List<Request> getRequests(){
		return requests;
	}

	public List<User> getUsers() {
		return users;
	}

	public List<Feedback> getFeedback() {
		return feedback;
	}
	
	public User getUserByEmail(String email){
		User u = null;
		try {
			u = new QueryUser(email).execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return u;
	}
	
	public User updateUserDevices(User user){
		User u = null;
		
		try {
			u= new UpdateUserDevice(user).execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return u;
		
	}
	
	public User insertUser(User user){
		User u = null;
		
		try {
			u= new InsertUser(user).execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return u;
		
	}
	
	public Request insertRequest(Request request){
		Request r = null;
		try {
			r = new InsertRequest(request).execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		requests.add(r);
		System.out.println("Aggiorno la view");
		System.out.println("Size: "+listeners.size());
		for (OnRequestLoadedListener l : listeners){
			System.out.println("Classe: "+l.getClass().toString());
			l.onRequestLoaded(requests);
		}
		return r;
	}
	
	

	public void addListener(OnRequestLoadedListener listener){
		listeners.add(listener);
		System.out.println("Ho aggiunto un listener: "+listener.getClass().toString());
	}

	public interface OnRequestLoadedListener{
		public void onRequestLoaded(List<Request> requests);
	}

	
	//Classi che effettuano le query
	private class LoadDataTask extends AsyncTask<Void, Void,  ArrayList<Request>> {

		@Override
		protected ArrayList<Request> doInBackground(Void... params) {
			System.out.println("Sto per ricostruire le request");
			try {
				users = (ArrayList<User>) manager.listUser().execute().getItems();
				try{
					for(User u : users){
						ArrayList<Request> a = (ArrayList<Request>) u.getRequests();
						if(a!=null && a.size()>0){
							for(Request r : a){
								Request req = manager.getRequest(r.getId()).execute();
								if(req==null) System.out.println("Non ho trovato nulla");
								else System.out.println("Title: "+req.getTitle());
								r.setOwner(u);}
							requests.addAll(a);
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					System.out.println("Like no tomorrow");}

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Requests null");
			}
			return (ArrayList<Request>)requests;
		}

		@Override
		protected void onPostExecute(ArrayList<Request> result) {
			System.out.println("NOTIFICO A TUTTI!("+listeners.size()+")");
			requests = result;
			for (OnRequestLoadedListener l : listeners){
				l.onRequestLoaded(requests);
			}
		}


	}
	private class QueryUser extends AsyncTask<Void, Void, User> {

		private String email;

		public QueryUser(String email){
			super();
			this.email = email;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			//showDialog();
		}

		@Override
		protected User doInBackground(Void... params) {
			System.out.println("Inizio la query");
			User result = null;
			try {
				result = manager.getUserByEmail(email).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result = null;
			}
			return result;
		}

		@Override
		@SuppressWarnings("null")
		protected void onPostExecute(User result) {
			if(result!=null)
				System.out.println("Ho ricevuto l'utente: "+result.getName());
			else
				System.out.println("Nessun utente ricevuto, devo crearlo.");
			//hideDialog();
			return;
		}

	}
	private class UpdateUserDevice extends AsyncTask<Void, Void, User> {

		private User u;

		public UpdateUserDevice(User u){
			super();
			this.u = u;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		//	showDialog();
		}

		@Override
		protected User doInBackground(Void... params) {
			System.out.println("Modifico la lista dei device");
			User result = null;
			if(u.getDevices()==null)
				u.setDevices(new ArrayList<String>());
			u.getDevices().add(LoginSession.getDeviceId());
			try {
				result = manager.updateUser(u).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result = null;
			}
			return result;
		}

	}
	private class InsertUser extends AsyncTask<Void, Void, User> {

		private User u;

		public InsertUser(User u){
			super();
			this.u = u;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			//showDialog();
		}

		@Override
		protected User doInBackground(Void... params) {
			System.out.println("Creo un nuovo utente");
			User result = null;
			try {
				result = manager.insertUser(u).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				result = null;
			}
			return result;
		}

	}
	private class InsertRequest extends AsyncTask<Void, Void, Request> {
		
		public Request r;
		
		public InsertRequest(Request r){
			this.r = r;
		}

		@Override
		protected Request doInBackground(Void... params) {
			User u = new User();
			try {
				u = manager.getUserByEmail(LoginSession.getUser().getEmail()).execute();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(u.getRequests()==null)
				u.setRequests(new ArrayList<Request>());
			u.getRequests().add(r);

			try {
				manager.updateUser(u).execute();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return r.setOwner(u);
		}



	}

}
