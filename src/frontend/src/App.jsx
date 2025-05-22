import React, { useState, useEffect } from 'react';
import { Actor, HttpAgent } from '@dfinity/agent';
import { Principal } from '@dfinity/principal';
import { AuthClient } from '@dfinity/auth-client'; // Import AuthClient

// Import your backend canister's IDL and canister ID
// *** IMPORTANT: Adjust the path 'declarations/backend' if necessary ***
import { idlFactory as backend_idl, canisterId as backend_canister_id } from 'declarations/backend';


function App() {
    // Authentication state
    const [authClient, setAuthClient] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [principal, setPrincipal] = useState(null);
    // Actor for interacting with the backend canister with authentication
    const [backendActor, setBackendActor] = useState(null);

    // Existing state - you might integrate or remove this later
    // const [owner, setOwner] = useState(null);
    // const [isConnected, setIsConnected] = useState(false);

    // Initialize AuthClient and check authentication status on load
    useEffect(() => {
        const initAuth = async () => {
            // Create an AuthClient
            const client = await AuthClient.create();
            setAuthClient(client);

            // Check if the user is already authenticated
            if (await client.isAuthenticated()) {
                handleAuthenticated(client);
            }
        };
        initAuth();

        // Optional: You can keep the basic IC connection check if still needed, but it might be redundant
        // const connectToIC = async () => {
        //     try {
        //         const agent = new HttpAgent({ host: 'http://localhost:8000' });
        //         await agent.fetchRootKey(); // TODO: Remove for mainnet deployment
        //         setIsConnected(true);
        //     } catch (error) {
        //         console.error('Failed to connect to IC:', error);
        //     }
        // };
        // connectToIC();

    }, []); // Empty dependency array means this effect runs only once on component mount

    // Function to handle the state updates after successful authentication
    const handleAuthenticated = async (client) => {
        const identity = client.getIdentity();
        const userPrincipal = identity.getPrincipal();
        setPrincipal(userPrincipal);
        setIsAuthenticated(true);

        // Create an authenticated actor for the backend canister
        // The agent uses the authenticated identity
        const agent = new HttpAgent({ identity, host: 'http://localhost:8000' }); // Use the authenticated identity
        // Fetch root key for local replica, remove for mainnet deployment
        // TODO: When deploying live, remove this line
        await agent.fetchRootKey();


        const backend = Actor.createActor(backend_idl, {
            agent,
            canisterId: backend_canister_id,
        });
        setBackendActor(backend);

        console.log("Authenticated with Principal:", userPrincipal.toString());
        // Now the backendActor can be used to call canister methods on behalf of the authenticated user
        // For example: await backendActor.getUserPoints();
    };

    // Function to initiate the Internet Identity login process
    const login = async () => {
        if (authClient) {
            await authClient.login({
                // Specify the identity provider URL
                identityProvider:
                    process.env.DFX_NETWORK === "ic"
                        ? "https://identity.ic0.app/#authorize" // Internet Identity canister on the IC mainnet
                        : `http://localhost:8000/?canisterId=${process.env.CANISTER_ID_INTERNET_IDENTITY}#authorize`, // Internet Identity canister on the local replica

                onSuccess: () => {
                    // Handle successful login
                    handleAuthenticated(authClient);
                },
                 // Optional: Specify a maximum time the login window can be open
                 // maxTimeToLive: BigInt(7 * 24 * 60 * 60 * 1000 * 1000 * 1000), // 7 days in nanoseconds
            });
        }
    };

    // Function to handle the logout process
    const logout = async () => {
        if (authClient && isAuthenticated) {
            await authClient.logout();
            setIsAuthenticated(false);
            setPrincipal(null);
            setBackendActor(null); // Clear the authenticated actor on logout
            console.log("Logged out");
            // Optionally redirect the user to a public page
        }
    };

    return (
        <div className="App">
            <header className="App-header">
                <h1>ICP Web3 Project</h1>
                {/* Display content based on authentication status */}
                {!isAuthenticated ? (
                    // Show login button if not authenticated
                    <button onClick={login}>Login with Internet Identity</button>
                ) : (
                    // Show authenticated content if authenticated
                    <div>
                        <p>Authenticated as: {principal.toString()}</p>
                        <button onClick={logout}>Logout</button>
                        {/*
                            TODO: Render components for data entry,
                            stress score display, feedback, history, and points here.
                            Pass backendActor to these components for canister interaction.
                        */}
                        <h2>Welcome!</h2>
                        <p>You are logged in. Now you can add your HRV data and check your stress levels.</p>
                        {/* Example of how you might pass the actor to a component */}
                        {/* <DataEntryForm backendActor={backendActor} /> */}
                        {/* <Dashboard backendActor={backendActor} principal={principal} /> */}

                    </div>
                )}
            </header>
        </div>
    );
}

export default App;