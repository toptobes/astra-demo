import os

import torch

DEVICE = os.getenv("ASTRA_DEMO_EMBEDDING_DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
DEVICE = torch.device(DEVICE)

DENSE_MODEL = os.getenv("ASTRA_DEMO_EMBEDDING_DENSE_MODEL", "intfloat/e5-base-v2")

APP_PORT = os.getenv('ASTRA_DEMO_EMBEDDING_SERVICE_PORT', "5000")

try:
	APP_PORT = int(APP_PORT)
except ValueError:
	exit("Environment variable MY_ENV_VAR is not a valid integer")
