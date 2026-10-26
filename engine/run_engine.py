"""Entry point used when the engine is packaged as a standalone program."""

import sys

import uvicorn

from chromacut_engine.main import app

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
    uvicorn.run(app, host="127.0.0.1", port=port, log_config=None)
