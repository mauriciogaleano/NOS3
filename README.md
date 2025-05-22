# ICP Web3 Project

This is a Web3 project built on the Internet Computer Protocol (ICP) using Motoko and React.

## Prerequisites

1. Install the DFINITY Canister SDK (DFX):
```bash
sh -ci "$(curl -fsSL https://internetcomputer.org/install.sh)"
```

2. Node.js and npm (Node Package Manager)

## Project Structure

```
├── src/
│   ├── backend/         # Motoko canister code
│   └── frontend/        # React frontend application
├── dfx.json            # DFINITY project configuration
└── README.md
```

## Setup Instructions

1. Clone the repository:
```bash
git clone <repository-url>
cd <project-directory>
```

2. Install frontend dependencies:
```bash
cd src/frontend
npm install
```

3. Start the local Internet Computer replica:
```bash
dfx start --background
```

4. Deploy the canisters:
```bash
dfx deploy
```

5. Start the frontend development server:
```bash
npm start
```

The application will be available at `http://localhost:8080`

## Development

- Backend (Motoko) code is in `src/backend/main.mo`
- Frontend (React) code is in `src/frontend/src`
- To make changes to the backend, edit the Motoko files and run `dfx deploy`
- Frontend changes will automatically reload in development mode

## Deployment

To deploy to the IC mainnet:

1. Create a cycles wallet and get some cycles
2. Configure your cycles wallet:
```bash
dfx identity --network ic set-wallet <WALLET_CANISTER_ID>
```

3. Deploy to mainnet:
```bash
dfx deploy --network ic
```

## License

MIT 