# PowerShell Script to handle the proctoring client handshake.
# This script is executed by the Inno Setup installer on the student's machine.

# --- Configuration ---
$port = 30123
$listenerPrefix = "http://localhost:$port/"
# The installer places this script in the same directory as the final JAR.
$scriptPath = $MyInvocation.MyCommand.Path
$appDir = Split-Path $scriptPath
$jarPath = Join-Path $appDir "ProctorApp.jar"

# --- Main Logic ---
try {
    # 1. Create and start an HTTP listener on the specified local port.
    $listener = New-Object System.Net.HttpListener
    $listener.Prefixes.Add($listenerPrefix)
    $listener.Start()
    # This Write-Host is for debugging if you run the script manually. It won't be visible to the user.
    Write-Host "Local handshake server started on $listenerPrefix. Waiting for connection from browser..."

    # 2. Wait for a single, valid POST request from the exam webpage's JavaScript.
    #    This command blocks (waits) until the student's browser sends the session ID.
    $context = $listener.GetContext()
    $request = $context.Request

    # 3. Add CORS headers to the response. This is MANDATORY for the browser to allow
    #    the JavaScript on the website to talk to this local server.
    $context.Response.Headers.Add("Access-Control-Allow-Origin", "*")
    $context.Response.Headers.Add("Access-Control-Allow-Methods", "POST, OPTIONS")
    $context.Response.Headers.Add("Access-Control-Allow-Headers", "Content-Type")

    # The browser first sends an "OPTIONS" request to check permissions (a "pre-flight" request).
    # We must respond successfully to this before it will send the real data.
    if ($request.HttpMethod -eq "OPTIONS") {
        $context.Response.StatusCode = 204 # No Content, which is the correct response.
        $context.Response.Close()
        # In a real server you might loop, but for the installer, we expect the POST right after.
        # We'll get the next context.
        $context = $listener.GetContext()
        $request = $context.Request
    }

    # Now, process the actual POST request with the data.
    if ($request.HttpMethod -ne "POST") {
        throw "Received invalid request method: $($request.HttpMethod)"
    }

    # 4. Read the session ID from the body of the request.
    $reader = New-Object System.IO.StreamReader($request.InputStream, $request.ContentEncoding)
    $body = $reader.ReadToEnd()
    $json = $body | ConvertFrom-Json
    $sessionId = $json.sessionId

    if (-not $sessionId) {
        throw "Session ID was not found in the request body."
    }

    # 5. Construct the full WebSocket URL and launch the main ProctorApp JAR.
    #    This is a simplified example. A more robust solution would get the host/port
    #    from the handshake request itself.
    $websocketUrl = "ws://localhost:8080/$sessionId" 

    Write-Host "Received sessionId: $sessionId. Launching ProctorApp with URL: $websocketUrl"

    # Use Start-Process to launch the Java application in the background so it doesn't block the installer.
    # The -WindowStyle Hidden flag ensures the student doesn't see a black console window pop up.
    Start-Process "java" -ArgumentList "-jar", "`"$jarPath`"", "`"$websocketUrl`"" -WindowStyle Hidden

    # 6. Send a success (200 OK) response back to the browser's JavaScript to let it know the handshake worked.
    $context.Response.StatusCode = 200
    $context.Response.Close()

    Write-Host "Handshake successful. ProctorApp launched."

}
catch {
    # If anything goes wrong, log the error.
    Write-Error "An error occurred during the handshake: $_"
    if ($listener -and $listener.IsListening) {
        $context.Response.StatusCode = 500 # Internal Server Error
        $context.Response.Close()
    }
}
finally {
    # 7. Always stop the listener and clean up when done.
    if ($listener -and $listener.IsListening) {
        $listener.Stop()
        Write-Host "Local handshake server stopped."
    }
}