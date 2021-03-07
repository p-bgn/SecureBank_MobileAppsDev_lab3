# SecureBank_MobileAppsDev_lab3



### Explain how you ensure user is the right one starting the app


LOGIN :

First, we ensure that the user is the one registered in the API 
because he needs to enter his name, last name and id (which, we suppose, could stand as a password).


FINGER PRINT : 

Then, using the phone's finger print security, we ensure that the user is the owner of the phone. 
So that if it gets stolen, the thief can't access the real phone's owner data.





### How do you securely save user's data on your phone ?


MASTER KEY :

To encrypt, we use a master key that we get from the AndroidKeyStore. 
This key is really efficient because as it is stored in a memory space outside the app, 
someone that has access to the source code doesn't has access to the key.
So eventually, he won't be able to get the data we store in the shared preferences.


ENCRYPTED SHARED DATA :

We use Shared Preferences to store the the last login and the last accounts.
We need this data to use the app offline.
In fact, when using the app offline, we can only login with the last 
first name, last name and ID we entered, and the data we see is the last one
we got from the API.
Encrypted Shared Preferences are very useful because it allows us to 
encrypt and decrypt the data directly with the master key and without having to 
code encrypt() and decrypt() functions.





### How did you hide the API url ?


First I tried to do it using shared preferences, 
but the problem is that they are not persistant from one phone to another,
so I realised I couldn't do it this way.

I thought about encrypting the URL with the master key and use this encrypted
URL in the code, but the problem is that the master also changes from one phone to another,
so this wasn't possible as well.

The only way I found to do it was to ask the users to enter both URLs on the first connection,
but I thought this wasn't realistic nor convenient for them, and I knew there was a better way 
to do it that I didn't see, therefor I avoided doing it this way.

So the URL is in the code in clear text, which of course is a problem 
but in the end, maybe better than a non-practical app.





### Application Screenshots ONLINE





### Application Screenshots OFFLINE















