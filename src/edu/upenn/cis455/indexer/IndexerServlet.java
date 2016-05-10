package edu.upenn.cis455.indexer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class IndexerServlet extends HttpServlet {
	static final long serialVersionUID = 455555001;
	Indexer indexer = new Indexer();

	public static void sop(Object o) {
		System.out.println(o);
	}

	@Override
	public void init(ServletConfig config) {
		String workingDirectory = System.getProperty("user.dir");
		System.out.println("Working Directory = " + workingDirectory);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
		String path = request.getPathInfo();
		PrintWriter out = response.getWriter();
		if (path.equals("/login")) {
			out.write("<!DOCTYPE html><html><body><form action=\"login\" method=\"post\">  name:<br>  <input type=\"text\" name=\"name\">  <br>  password:<br>  <input type=\"text\" name=\"password\">  <br><br>  <input type=\"submit\" value=\"submit\"></form> </body></html>");
			return;
		} else if (path.equals("/logout")) {
			HttpSession session=request.getSession();  
            session.invalidate();
			printMessage(out, "You are successfully logged out!");
            return;
		}
		
		HttpSession session = request.getSession(false);  
        if (session == null) {  
        	printMessage(out, "Please login first");
        	return;
        }  
        
	    if (path.equals("/")) {
			out.write("<!DOCTYPE html><html><body><a href=\"start_indexing\">start_indexing</a><br><a href=\"stop_indexing\">stop_indexing</a><br><a href=\"login\">login</a><br><a href=\"logout\">logout</a> </body></html>");
		} else if (path.equals("/parse_content_form")) {
			out.write("<!DOCTYPE html><html><body><form action=\"parse_content\" method=\"post\">  url:<br>  <input type=\"text\" name=\"url\">  <br>  content:<br>  <input type=\"text\" name=\"content\">  <br><br>  <input type=\"submit\" value=\"click to submit\"></form> </body></html>");
		} else if (path.equals("/start_indexing")) {
			indexer.startIndexing();
			printMessage(out, "ok");
		} else if (path.equals("/stop_indexing")) {
			indexer.stopIndexing();
			printMessage(out, "ok");
		} else {
			printMessage(out, "Invalid get path: " + path);
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
		String path = request.getPathInfo();
		PrintWriter out = response.getWriter();
		if (path.equals("/login")) {
			String name = request.getParameter("name");  
	        String password = request.getParameter("password");
	        if (validUser(name, password)) {
	        	request.getSession().setAttribute("name", name);
	        	response.getWriter().write("welcome!");
	        	response.sendRedirect("/");
	        } else {
				printMessage(out, "Wrong username or password!");	        	
	        }
	        return;
		} 
		
		HttpSession session = request.getSession(false);  
        if (session == null) {  
        	printMessage(out, "Please login first");
        	return;
        }  

        else if (path.equals("/parse_content")) {
			String url = request.getParameter("url");
			String content = request.getParameter("content");
			parseContent(url, content);
		} else {
			printMessage(out, "Invalid post path: " + path);
		}
	}

	void printMessage(PrintWriter out, String message) {
		out.println("<!DOCTYPE html><html><body><p>" + message + "</p></body></html>");
	}
	
	boolean validUser(String name, String password) {
		return password.equals("123qweasdzxc");
	}
	
	public void parseContent(String url, String content) {
	}	
}
