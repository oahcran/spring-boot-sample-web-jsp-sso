<!DOCTYPE html>

<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html lang="en">

<body>

  <p>You've used the authcode flow! Here's the result of calling /userinfo:</p>
  <br>
  Response: ${response} 
  <br>
  
  <p>This is the Access Token that was used:</p>
  <br>
  AccessToken: ${access_token} 
  <br> 
  
  <p>This is the ID Token:</p>
  <br>
  ID Token: ${id_token} 
  <br> 
	
</body>

</html>
