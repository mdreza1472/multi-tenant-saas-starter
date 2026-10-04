#!/usr/bin/env python3
import aws_cdk as cdk

from saas_starter_stack import SaasStarterStack

app = cdk.App()
SaasStarterStack(app, "SaasStarterStack")
app.synth()
