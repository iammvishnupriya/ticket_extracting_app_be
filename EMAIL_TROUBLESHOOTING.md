# Email Connection Troubleshooting Guide

## Common Issues and Solutions

### 1. Authentication Failures

**Problem**: `AuthenticationFailedException`

**Solutions**:
- ✅ **Use App Password**: For Office 365/Outlook, create an app-specific password
- ✅ **Enable Less Secure App Access**: (if using regular password)
- ✅ **Check MFA**: Disable Multi-Factor Authentication or use app password
- ✅ **Verify Credentials**: Double-check username and password

### 2. Connection Timeouts

**Problem**: Connection timeouts or network errors

**Solutions**:
- ✅ **Firewall**: Check corporate firewall settings
- ✅ **Port Access**: Ensure port 993 is accessible
- ✅ **SSL/TLS**: Verify SSL certificate settings
- ✅ **Network**: Test from command line: `telnet outlook.office365.com 993`

### 3. SSL/TLS Issues

**Problem**: SSL handshake failures

**Solutions**:
- ✅ **Java Version**: Use Java 11+ with updated SSL protocols
- ✅ **Trust Store**: Add Outlook certificates to Java trust store
- ✅ **Protocol**: Ensure TLS 1.2+ is enabled

### 4. Office 365 Specific Issues

**Office 365 Requirements**:
- ✅ **Modern Authentication**: May require OAuth2 instead of basic auth
- ✅ **Exchange Online**: Ensure IMAP is enabled for the mailbox
- ✅ **Tenant Policies**: Check organization email policies

### 5. Testing Steps

1. **Test Connection**:
   ```bash
   curl -X GET http://localhost:8080/api/emails/test-connection
   ```

2. **Check Logs**:
   ```bash
   tail -f logs/application.log
   ```

3. **Manual IMAP Test**:
   ```bash
   openssl s_client -connect outlook.office365.com:993
   ```

### 6. Configuration Checklist

- [ ] Correct IMAP server: `outlook.office365.com`
- [ ] Correct port: `993`
- [ ] SSL enabled: `true`
- [ ] Authentication enabled: `true`
- [ ] Valid credentials
- [ ] IMAP enabled in Office 365
- [ ] App password created (if MFA enabled)

### 7. Alternative Authentication (OAuth2)

For production environments, consider implementing OAuth2:

```java
// Future enhancement: OAuth2 authentication
props.put("mail.imap.auth.mechanisms", "XOAUTH2");
```

### 8. Environment Variables

For security, use environment variables:

```bash
export EMAIL_USERNAME="your-email@hepl.com"
export EMAIL_PASSWORD="your-app-password"
```

### 9. Corporate Network Issues

If behind corporate proxy:
- Configure proxy settings in application.properties
- Add proxy authentication if required
- Whitelist outlook.office365.com domains

### 10. Monitoring and Alerts

Enable health checks:
- Email connection monitoring
- Failed authentication alerts
- Network connectivity checks